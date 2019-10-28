/*
 * Copyright 2019 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.tests.smokeTests.confluent;

import io.apicurio.tests.BaseIT;
import io.apicurio.tests.utils.subUtils.GlobalRuleUtils;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RulesResourceConfluentIT extends BaseIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataConfluentIT.class);

    @Test
    void compatibilityGlobalRules() throws IOException, RestClientException {
        GlobalRuleUtils.createGlobalCompatibilityConfig("FULL");

        Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"myrecord1\",\"fields\":[{\"name\":\"foo\",\"type\":\"string\"}]}");

        String schemeSubject = "subject-example";
        int schemaId = confluentService.register(schemeSubject, schema);

        confluentService.getById(schemaId);

        Schema newSchema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"myrecord2\",\"fields\":[{\"name\":\"foo\",\"type\":\"string\"}]}");

        confluentService.register(schemeSubject, newSchema);

        LOGGER.info("Checking 'Compability with same scheme' and expected code {}", 200);
        GlobalRuleUtils.testCompatibility("{\"schema\":\"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"myrecord2\\\",\\\"fields\\\":[{\\\"name\\\":\\\"foo\\\",\\\"type\\\":\\\"string\\\"}]}\"}", schemeSubject, 200);

        LOGGER.info("Checking 'Subject not found' and expected code {}", 404);
        GlobalRuleUtils.testCompatibility("{\"schema\":\"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"myrecord2\\\",\\\"fields\\\":[{\\\"name\\\":\\\"foo\\\",\\\"type\\\":\\\"string\\\"}]}\"}", "subject-not-found", 404);

        LOGGER.info("Checking 'Invalid avro format' and expected code {}", 400);
        GlobalRuleUtils.testCompatibility("{\"type\":\"INVALID\",\"config\":\"invalid\"}", schemeSubject, 400);
    }
}