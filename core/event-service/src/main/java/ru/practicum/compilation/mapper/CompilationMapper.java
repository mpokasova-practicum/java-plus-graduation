package ru.practicum.compilation.mapper;

import org.mapstruct.*;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationDto newCompilationDto);

    CompilationDto toDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCompilationFromRequest(UpdateCompilationRequest request, @MappingTarget Compilation compilation);
}