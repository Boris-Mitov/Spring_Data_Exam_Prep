package com.example.football.service.impl;

import com.example.football.models.dto.TownImportDto;
import com.example.football.models.entity.Town;
import com.example.football.repository.TownRepository;
import com.example.football.service.TownService;
import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static com.example.football.util.Paths.TOWNS_JSON_PATH;

@Service
public class TownServiceImpl implements TownService {

    private final TownRepository townRepository;

    private final ModelMapper modelMapper;

    private final Gson gson;

    private final Validator validator;

    @Autowired
    public TownServiceImpl(TownRepository townRepository, ModelMapper modelMapper, Gson gson, Validator validator) {
        this.townRepository = townRepository;
        this.modelMapper = modelMapper;
        this.gson = gson;
        this.validator = validator;
    }

    @Override
    public boolean areImported() {
        return this.townRepository.count() > 0;
    }

    @Override
    public String readTownsFileContent() throws IOException {
        return Files.readString(TOWNS_JSON_PATH);
    }

    @Override
    public String importTowns() throws IOException {
        String jsonTowns = readTownsFileContent();

        TownImportDto[] townImportDtos = this.gson.fromJson(jsonTowns, TownImportDto[].class);

        List<String> result = new ArrayList<>();

        for (TownImportDto townImportDto : townImportDtos) {
            Set<ConstraintViolation<TownImportDto>> errors =
                    this.validator.validate(townImportDto);

            if(errors.isEmpty()) {
                Optional<Town> optionalTown = this.townRepository.findByName(townImportDto.getName());

                if(optionalTown.isEmpty()) {
                    Town town = this.modelMapper.map(townImportDto, Town.class);

                    this.townRepository.save(town);

                    result.add(String.format("Successfully imported Town %s - %d",
                            town.getName(),
                            town.getPopulation()));

                } else {
                    result.add("Invalid Town");
                }
            } else {
                result.add("Invalid Town");
            }
        }

        return String.join(System.lineSeparator(), result);
    }
}
