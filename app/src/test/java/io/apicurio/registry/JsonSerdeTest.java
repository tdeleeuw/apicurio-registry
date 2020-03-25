/*
 * Copyright 2020 Red Hat
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

package io.apicurio.registry;

import io.apicurio.registry.client.RegistryService;
import io.apicurio.registry.rest.beans.ArtifactMetaData;
import io.apicurio.registry.support.Person;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.utils.ConcurrentUtil;
import io.apicurio.registry.utils.serde.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.utils.serde.JsonSchemaKafkaSerializer;
import io.apicurio.registry.utils.serde.strategy.SimpleTopicIdStrategy;
import io.apicurio.registry.utils.tests.RegistryServiceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Assertions;

import static io.apicurio.registry.utils.tests.TestUtils.retry;

import java.io.InputStream;
import java.util.concurrent.CompletionStage;

/**
 * @author Ales Justin
 */
@QuarkusTest
public class JsonSerdeTest extends AbstractResourceTestBase {

    @RegistryServiceTest
    public void testSchema(RegistryService service) throws Exception {
        InputStream jsonSchema = getClass().getResourceAsStream("/io/apicurio/registry/util/json-schema.json");
        Assertions.assertNotNull(jsonSchema);

        String artifactId = generateArtifactId();

        CompletionStage<ArtifactMetaData> cs = service.createArtifact(ArtifactType.JSON, artifactId, jsonSchema);
        ArtifactMetaData amd = ConcurrentUtil.result(cs);

        // make sure we have schema registered
        service.reset();
        retry(() -> service.getArtifactByGlobalId(amd.getGlobalId()));

        Person person = new Person("Ales", "Justin", 23);

        JsonSchemaKafkaSerializer<Person> serializer = new JsonSchemaKafkaSerializer<>(service, true);
        serializer.setArtifactIdStrategy(new SimpleTopicIdStrategy<>());

        Headers headers = new RecordHeaders();
        byte[] bytes = serializer.serialize(artifactId, headers, person);

        JsonSchemaKafkaDeserializer<Person> deserializer = new JsonSchemaKafkaDeserializer<>(service, true);

        person = deserializer.deserialize(artifactId, headers, bytes);

        Assertions.assertEquals("Ales", person.getFirstName());
        Assertions.assertEquals("Justin", person.getLastName());
        Assertions.assertEquals(23, person.getAge());

        person.setAge(-1);

        try {
            serializer.serialize(artifactId, new RecordHeaders(), person);
            Assertions.fail();
        } catch (Exception ignored) {
        }

        serializer.setValidationEnabled(false); // disable validation
        // create invalid person bytes
        bytes = serializer.serialize(artifactId, headers, person);

        try {
            deserializer.deserialize(artifactId, headers, bytes);
            Assertions.fail();
        } catch (Exception ignored) {
        }
    }
}