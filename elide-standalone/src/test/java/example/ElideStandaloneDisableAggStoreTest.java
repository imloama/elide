/*
 * Copyright 2020, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsInAnyOrder;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneAsyncSettings;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import example.models.Post;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Properties;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneDisableAggStoreTest extends ElideStandaloneTest {

    @BeforeAll
    public void init() throws Exception {
        elide = new ElideStandalone(new ElideStandaloneSettings() {

            @Override
            public Properties getDatabaseProperties() {
                Properties options = new Properties();

                options.put("hibernate.show_sql", "true");
                options.put("hibernate.hbm2ddl.auto", "create");
                options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                options.put("hibernate.current_session_context_class", "thread");
                options.put("hibernate.jdbc.use_scrollable_resultset", "true");

                options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                options.put("javax.persistence.jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;");
                options.put("javax.persistence.jdbc.user", "sa");
                options.put("javax.persistence.jdbc.password", "");
                return options;
            }

            @Override
            public String getModelPackageName() {
                return Post.class.getPackage().getName();
            }

            @Override
            public boolean enableSwagger() {
                return true;
            }

            @Override
            public boolean enableGraphQL() {
                return true;
            }

            @Override
            public boolean enableJSONAPI() {
                return true;
            }

            @Override
            public ElideStandaloneAsyncSettings getAsyncProperties() {
                ElideStandaloneAsyncSettings asyncPropeties = new ElideStandaloneAsyncSettings() {
                    @Override
                    public boolean enabled() {
                        return true;
                    }

                    @Override
                    public boolean enableCleanup() {
                        return true;
                    }

                    @Override
                    public Integer getThreadSize() {
                        return 5;
                    }

                    @Override
                    public Integer getMaxRunTimeSeconds() {
                        return 1800;
                    }

                    @Override
                    public Integer getQueryCleanupDays() {
                        return 3;
                    }
                };
                return asyncPropeties;
            }

            @Override
            public ElideStandaloneAnalyticSettings getAnalyticProperties() {
                ElideStandaloneAnalyticSettings analyticPropeties = new ElideStandaloneAnalyticSettings() {
                    @Override
                    public boolean enableDynamicModelConfig() {
                        return true;
                    }

                    @Override
                    public boolean enableAggregationDataStore() {
                        return false;
                    }

                    @Override
                    public String getDynamicConfigPath() {
                        return "src/test/resources/configs/";
                    }
                };
                return analyticPropeties;
            }
        });
        elide.start(false);
    }

    @Override
    @Test
    public void swaggerDocumentTest() {
        when()
        .get("/swagger/doc/test")
         .then()
         .statusCode(200)
         .body("tags.name", containsInAnyOrder("post", "asyncQuery", "tableExport"));
    }

    @Override
    @Test
    public void testJsonAPIPost() {
        given()
        .contentType(JSONAPI_CONTENT_TYPE)
        .accept(JSONAPI_CONTENT_TYPE)
        .body(
            datum(
                resource(
                    type("post"),
                    id("1"),
                    attributes(
                        attr("content", "This is my first post. woot."),
                        attr("date", "2019-01-01T00:00Z")
                    )
                )
            )
        )
        .post("/api/v1/post")
        .then()
        .statusCode(HttpStatus.SC_CREATED)
        .extract().body().asString();
    }
}
