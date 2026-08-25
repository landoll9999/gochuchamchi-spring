package com.gochuchamchi.controller;

import com.gochuchamchi.dto.NoticeDto;
import com.gochuchamchi.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@Profile("web")
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeMapper noticeMapper;
    private static final int PAGE_SIZE = 15;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "week") String period,
                       @RequestParam(defaultValue = "title") String type,
                       @RequestParam(defaultValue = "") String keyword,
                       Model model) {
        int offset = (page - 1) * PAGE_SIZE;
        int total = noticeMapper.countAll(keyword, type, period);
        model.addAttribute("notices", noticeMapper.findAll(offset, PAGE_SIZE, keyword, type, period));
        model.addAttribute("totalCount", total);
        model.addAttribute("totalPages", (int) Math.ceil((double) total / PAGE_SIZE));
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        model.addAttribute("period", period);
        return "notice/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        NoticeDto notice = noticeMapper.findById(id);
        if (notice == null) return "redirect:/notice";
        noticeMapper.incrementViews(id);
        model.addAttribute("notice", notice);
        return "notice/detail";
    }
}
