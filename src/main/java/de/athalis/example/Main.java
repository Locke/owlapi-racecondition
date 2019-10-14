package de.athalis.example;


import org.semanticweb.owlapi.apibinding.OWLManager;

import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.PriorityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    private static final URL ontologyFile = Main.class.getClassLoader().getResource("empty.owl");

    public static void main(String[] args) throws InterruptedException {
        assert ontologyFile != null;
        
        final Logger logger = LoggerFactory.getLogger(Main.class);

        logger.info("Starting...");

        final int parallelism = Runtime.getRuntime().availableProcessors() * 4 * 2;
        ExecutorService execService = Executors.newFixedThreadPool(parallelism);

        logger.debug("ThreadPool("+parallelism+") created");

        final int numReader = parallelism * 16 * 2;
        List<Callable<OWLOntology>> tasks = new ArrayList<>(numReader);

        for (int i = 0; i < numReader; i++) {
            tasks.add(new ReadTask());
        }

        logger.debug(numReader + " Readers prepared, invoke them...");

        execService.invokeAll(tasks);

        logger.info(numReader + " Readers invoked.");

        execService.shutdown();
        boolean terminated = execService.awaitTermination(5, TimeUnit.MINUTES);

        logger.info("execService terminated: " + terminated);
    }

    private static class ReadTask implements Callable<OWLOntology> {

        @Override
        public OWLOntology call() {
            final Logger logger = LoggerFactory.getLogger(ReadTask.class);
            
            logger.info("starting...");
            final OWLOntologyManager manager = OWLManager.createConcurrentOWLOntologyManager();

            PriorityCollection<OWLParserFactory> parsers = manager.getOntologyParsers();
            logger.info("number of parsers: " + parsers.size() + " (pre)");

            if (parsers.size() < 19) {
                logger.warn("expecting failure...");
            }

            final OWLOntology ont;
            try {
                ont = manager.loadOntologyFromOntologyDocument(ontologyFile.openStream());
                logger.debug("done.");
                return ont;
            } catch (OWLOntologyCreationException | IOException e) {
                logger.error("loadOntologyFromOntologyDocument failed", e);
            }
            return null;
        }
    }
}
