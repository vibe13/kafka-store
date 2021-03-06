/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.kafkastore.kafka;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.pnc.kafkastore.mapper.BuildStageRecordMapper;
import org.jboss.pnc.kafkastore.model.BuildStageRecord;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Consume from a Kafka topic, parse the data and store it in the database
 */
@ApplicationScoped
@Slf4j
public class Consumer {

    @Inject
    BuildStageRecordMapper mapper;

    /**
     * Main method to consume information from a Kafka topic into the database.
     *
     * @param jsonString
     * @throws Exception
     */
    @Incoming("duration")
    public void consume(String jsonString) throws Exception {
        System.out.print(".");

        // log.info("Incoming: {}", jsonString);
        Optional<BuildStageRecord> buildStageRecord = mapper.mapKafkaMsgToBuildStageRecord(jsonString);

        // TODO: handle error better
        // do this because method running in an IO thread and we can only store a POJO in a worker thread
        buildStageRecord.ifPresent(br -> {
            log.info(br.toString());
            CompletableFuture.runAsync(() -> store(br)).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        });
    }

    @Transactional
    void store(BuildStageRecord message) {
        message.persist();
    }
}
