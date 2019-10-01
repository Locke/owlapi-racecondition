package de.athalis.example;

import org.semanticweb.owlapi.apibinding.OWLManager;

import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    private static final URL ontologyFile = Main.class.getClassLoader().getResource("standard_PASS_ont_v_1.0.0.owl");

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        assert ontologyFile != null;

        System.out.println("Starting...");

        final int parallelism = Runtime.getRuntime().availableProcessors() * 4;
        ExecutorService execService = Executors.newFixedThreadPool(parallelism);

        System.out.println("ThreadPool("+parallelism+") created");

        final int numReader = parallelism * 16;
        List<Callable<OWLOntology>> tasks = new ArrayList<>(numReader);

        for (int i = 0; i < numReader; i++) {
            tasks.add(new ReadTask());
        }

        System.out.println(numReader + " Readers prepared");

        execService.invokeAll(tasks);

        System.out.println(numReader + " Readers submitted, awaiting completion within 5 minutes...");

        execService.shutdown();
        boolean terminated = execService.awaitTermination(5, TimeUnit.MINUTES);

        System.out.println("execService terminated: " + terminated);
    }

    private static class ReadTask implements Callable<OWLOntology> {

        @Override
        public OWLOntology call() {
            System.out.println(Thread.currentThread() + ": called...");
            final OWLOntologyManager manager = OWLManager.createConcurrentOWLOntologyManager();

            PriorityCollection<OWLParserFactory> parsers = manager.getOntologyParsers();
            System.out.println(Thread.currentThread() + ": parsers: " + parsers.size() + " (pre)");

            final OWLOntology ont;
            try {
                ont = manager.loadOntologyFromOntologyDocument(ontologyFile.openStream());
                System.out.println(Thread.currentThread() + ": done.");
                return ont;
            } catch (OWLOntologyCreationException e) {
                System.err.println(Thread.currentThread() + ": OWLOntologyCreationException!");

                PriorityCollection<OWLParserFactory> parsers2 = manager.getOntologyParsers();
                System.err.println(Thread.currentThread() + ": parsers: " + parsers2.size() + " (post)");

                //e.printStackTrace();
            } catch (IOException e) {
                System.err.println(Thread.currentThread() + ": IOException!");
                //e.printStackTrace();
            }
            return null;
        }
    }
}
