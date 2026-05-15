package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return compilations.stream()
                .map(compilationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id = " + compId + " не найдена"));

        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConflictException("Подборка с наименованием " + newCompilationDto.getTitle() + " уже существует");
        }
        Compilation compilation = compilationMapper.toEntity(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllByEventIds(new ArrayList<>(newCompilationDto.getEvents()));

            if (events.size() != newCompilationDto.getEvents().size()) throw new NotFoundException("Не все события найдены");
            compilation.setEvents(new HashSet<>(events));

        } else compilation.setEvents(new HashSet<>());

        Compilation savedCompilation = compilationRepository.save(compilation);
        return compilationMapper.toDto(savedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        try {
            compilationRepository.deleteById(compId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Подборка с id = " + compId + " не найдена");
        }
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id = " + compId + " не найдена"));

        if (updateCompilationRequest.getTitle() != null
                && !updateCompilationRequest.getTitle().equals(compilation.getTitle())
                && compilationRepository.existsByTitle(updateCompilationRequest.getTitle())) {
            throw new ConflictException("Подборка с названием \"" + updateCompilationRequest.getTitle() + "\" уже существует");
        }

        if (updateCompilationRequest.getEvents() != null) {
            if (updateCompilationRequest.getEvents().isEmpty()) {
                compilation.setEvents(new HashSet<>());
            } else {
                List<Event> events = eventRepository.findAllByEventIds(new ArrayList<>(updateCompilationRequest.getEvents()));
                if (events.size() != updateCompilationRequest.getEvents().size()) {
                    throw new NotFoundException("Некоторые события не найдены");
                }
                compilation.setEvents(new HashSet<>(events));
            }
        }

        compilationMapper.updateCompilationFromRequest(updateCompilationRequest, compilation);

        return compilationMapper.toDto(compilationRepository.save(compilation));
    }
}
