package com.beton408.controller;

import com.beton408.entity.UserEntity;
import com.beton408.entity.FaqEntity;
import com.beton408.entity.HistoryEntity;
import com.beton408.model.HistoryDto;
import com.beton408.repository.HistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@CrossOrigin(value = "*")
public class HistoryController {
    @Autowired
    private HistoryRepository historyRepository;

    @GetMapping("/{userId}/chat")
    public Page<HistoryDto> getUserFaqs(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(defaultValue = "id") String sortBy,
                                        @RequestParam(defaultValue = "ASC") String sortDir,
                                        @RequestParam(required = false, defaultValue = "") String searchTerm) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable paging = PageRequest.of(page, size, sort);
        Specification<HistoryEntity> spec = Specification.where(null);

        if (!searchTerm.isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) -> {
                String pattern = "%" + searchTerm + "%";
                return criteriaBuilder.or(
                        criteriaBuilder.like(root.get("question"), pattern)
                );
            });
        }
        if (userId != null) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("user").get("id"), userId);
            });
        }

        Page<HistoryEntity> historyEntities = historyRepository.findAll(spec, paging);

        return historyEntities.map(HistoryDto::fromEntity);
    }


    @PostMapping("/chat/{userId}/faq/{faqId}")
    public HistoryEntity createUserFaq(@PathVariable Long userId, @PathVariable Long faqId) {
        HistoryEntity userFaq = new HistoryEntity();
        UserEntity user = new UserEntity();
        user.setId(userId);
        userFaq.setUser(user);
        FaqEntity faq = new FaqEntity();
        faq.setId(faqId);
        userFaq.setFaq(faq);
        return historyRepository.save(userFaq);
    }
    @GetMapping("/chart")
    public ResponseEntity<Map<String, Object>> getChart() {
        List<HistoryEntity> historyEntities = historyRepository.findAll();
        Map<LocalDate, Long> groupedByDate = historyEntities.stream()
                .collect(Collectors.groupingBy(
                        historyEntity -> historyEntity.getCreatedAt().toLocalDate(),
                        Collectors.counting()));

        List<String> labels = groupedByDate.keySet().stream()
                .sorted()
                .map(date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .collect(Collectors.toList());

        List<Long> data = groupedByDate.values().stream().collect(Collectors.toList());

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("data", data);

        return ResponseEntity.ok().body(chartData);
    }



}

