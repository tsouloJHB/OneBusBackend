package com.backend.onebus.controller;

import com.backend.onebus.dto.BusNumberCreateDTO;
import com.backend.onebus.dto.BusNumberResponseDTO;
import com.backend.onebus.model.BusCompany;
import com.backend.onebus.model.BusNumber;
import com.backend.onebus.repository.BusCompanyRepository;
import com.backend.onebus.repository.BusNumberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
public class BusNumberControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BusCompanyRepository busCompanyRepository;

    @Autowired
    private BusNumberRepository busNumberRepository;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testCreateBusNumberWithBusCompanyId() throws Exception {
        // Create a bus company first with unique company code
        BusCompany busCompany = new BusCompany();
        busCompany.setName("Test Company");
        busCompany.setRegistrationNumber("REG" + (System.currentTimeMillis() % 10000)); // Unique registration number
        busCompany.setCompanyCode("TC" + (System.currentTimeMillis() % 1000)); // Unique code, max 6 chars
        busCompany.setEmail("test@company.com");
        busCompany.setPhone("1234567890");
        busCompany.setAddress("123 Test Street");
        busCompany.setCity("Test City");
        busCompany.setIsActive(true);

        BusCompany savedCompany = busCompanyRepository.save(busCompany);

        String uniqueBusNumber = "101-" + System.currentTimeMillis();
        
        // Create bus number DTO with unique bus number
        BusNumberCreateDTO createDTO = new BusNumberCreateDTO();
        createDTO.setBusNumber(uniqueBusNumber); // Unique bus number
        createDTO.setBusCompanyId(savedCompany.getId());
        createDTO.setRouteName("Test Route");
        createDTO.setDescription("Test bus number");
        createDTO.setStartDestination("Start Point");
        createDTO.setEndDestination("End Point");
        createDTO.setDirection("Northbound");
        createDTO.setDistanceKm(10.5);
        createDTO.setEstimatedDurationMinutes(30);
        createDTO.setFrequencyMinutes(15);
        createDTO.setIsActive(true);

        // Perform POST request
        mockMvc.perform(post("/api/bus-numbers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.busNumber").value(uniqueBusNumber))
                .andExpect(jsonPath("$.busCompanyId").value(savedCompany.getId()))
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.routeName").value("Test Route"))
                .andExpect(jsonPath("$.isActive").value(true));

        // Verify in database
        Optional<BusNumber> savedBusNumber = busNumberRepository.findByBusNumberAndBusCompany_Id(uniqueBusNumber, savedCompany.getId());
        assert(savedBusNumber.isPresent());
        assert(savedBusNumber.get().getBusCompany().getId().equals(savedCompany.getId()));
    }
}
