package org.oransc.rappmanager.r1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class R1InterfaceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readConfigurationReturnsSampleMoi() throws Exception {
        mockMvc.perform(get("/ran-oam-cm/v1/SubNetwork=SN1,GNBDUFunction=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administrativeState").value("UNLOCKED"));
    }

    @Test
    void discoverModelsReturnsSampleCatalog() throws Exception {
        mockMvc.perform(get("/ai-ml-model-discovery/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void discoverModelsFiltersByNameAndVersion() throws Exception {
        mockMvc.perform(get("/ai-ml-model-discovery/v1/models")
                        .param("model-name", "qos-predictor")
                        .param("model-version", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].modelId.modelName").value("qos-predictor"));
    }

    @Test
    void patchConfigurationUpdatesAttribute() throws Exception {
        mockMvc.perform(patch("/ran-oam-cm/v1/SubNetwork=SN1,GNBDUFunction=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modifications": [
                                    {
                                      "modifyOperator": "REPLACE",
                                      "path": "priorityLabel",
                                      "value": 5
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorityLabel").value(5));
    }
}
