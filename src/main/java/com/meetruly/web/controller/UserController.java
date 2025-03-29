package com.meetruly.web.controller;

import com.meetruly.core.constant.Gender;
import com.meetruly.core.exception.MeetrulyException;
import com.meetruly.matching.service.MatchingService;
import com.meetruly.user.dto.*;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import com.meetruly.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final MatchingService matchingService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showUserProfile(Model model, Principal principal) {

        String username = principal.getName();
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new MeetrulyException("User not found: " + username));

        Optional<UserProfileDto> profileOpt = userService.getUserProfile(user.getId());

        if (profileOpt.isEmpty()) {
            model.addAttribute("profile", new UserProfileDto());
            model.addAttribute("isNewProfile", true);
        } else {
            model.addAttribute("profile", profileOpt.get());
            model.addAttribute("isNewProfile", false);
        }

        model.addAttribute("user", user);

        return "user/profile";
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String updateUserProfile(@Valid @ModelAttribute("profile") UserProfileDto profileDto,
                                    BindingResult result, Principal principal,
                                    RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "user/profile";
        }

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            profileDto.setUserId(user.getId());

            UserProfile savedProfile = userService.createOrUpdateProfile(user.getId(), profileDto);

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
            return "redirect:/profile";
        } catch (MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/profile";
        }
    }

    @PostMapping("/upload-image")
    @PreAuthorize("isAuthenticated()")
    public String uploadProfileImage(@RequestParam("image") MultipartFile image,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {

        if (image.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select an image to upload");
            return "redirect:/profile";
        }

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            String applicationPath = new File("").getAbsolutePath();

            String uploadDirRelative = "uploads/profile-images/" + user.getId();
            String uploadDir = applicationPath + File.separator + uploadDirRelative;
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);


            String imageUrl = request.getContextPath() + "/uploads/profile-images/" + user.getId() + "/" + fileName;

            Optional<UserProfileDto> profileOpt = userService.getUserProfile(user.getId());

            if (profileOpt.isPresent()) {
                UserProfileDto profileDto = profileOpt.get();
                profileDto.setProfileImageUrl(imageUrl);
                userService.createOrUpdateProfile(user.getId(), profileDto);
            } else {

                UserProfileDto profileDto = new UserProfileDto();
                profileDto.setUserId(user.getId());
                profileDto.setProfileImageUrl(imageUrl);
                userService.createOrUpdateProfile(user.getId(), profileDto);
            }

            redirectAttributes.addFlashAttribute("uploadedImageUrl", imageUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Profile image uploaded successfully");
            return "redirect:/profile";
        } catch (IOException | MeetrulyException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload image: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public String showSearchForm(Model model) {
        model.addAttribute("searchRequest", new SearchProfileRequest());
        return "user/search";
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public String searchProfiles(@ModelAttribute("searchRequest") SearchProfileRequest searchRequest,
                                 Model model, Principal principal) {

        try {

            String username = principal.getName();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            if (userService.hasReachedProfileViewLimit(user.getId())) {
                model.addAttribute("errorMessage",
                        "You have reached your profile view limit. Please upgrade your subscription plan.");
                return "user/search";
            }

            SearchProfileResponse searchResult = userService.searchProfiles(searchRequest);

            searchResult.getProfiles().forEach(profile -> {

                profile.setBlurredImage(!userService.canViewFullProfile(user.getId()));
            });

            model.addAttribute("searchResult", searchResult);
            model.addAttribute("searchRequest", searchRequest);

            return "user/search-results";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/search";
        }
    }


    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public String viewUserProfile(@PathVariable("userId") UUID userId, Model model, Principal principal) {
        try {

            String username = principal.getName();
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            if (!userId.equals(currentUser.getId()) &&
                    userService.hasReachedProfileViewLimit(currentUser.getId())) {
                model.addAttribute("errorMessage",
                        "You have reached your profile view limit. Please upgrade your subscription plan.");
                return "redirect:/profile/search";
            }

            User user = userService.getUserById(userId);

            Optional<UserProfileDto> profileOpt = userService.getUserProfile(userId);

            UserProfileDto profileDto;
            if (profileOpt.isEmpty()) {

                profileDto = new UserProfileDto();
                profileDto.setUserId(userId);

            } else {
                profileDto = profileOpt.get();
            }

            if (!userId.equals(currentUser.getId())) {
                userService.incrementProfileViewCount(currentUser.getId());
            }

            boolean canViewFullProfile = userId.equals(currentUser.getId()) ||
                    userService.canViewFullProfile(currentUser.getId());

            model.addAttribute("profile", profileDto);
            model.addAttribute("user", user);
            model.addAttribute("canViewFullProfile", canViewFullProfile);
            model.addAttribute("isOwnProfile", userId.equals(currentUser.getId()));

            return "user/view-profile";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/profile/search";
        }
    }

    @GetMapping("/gallery/{gender}")
    @PreAuthorize("isAuthenticated()")
    public String viewProfileGallery(@PathVariable("gender") String genderStr,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "12") int size,
                                     Model model, Principal principal) {

        try {

            String username = principal.getName();
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new MeetrulyException("User not found: " + username));

            Gender gender;
            try {
                gender = Gender.valueOf(genderStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                model.addAttribute("errorMessage", "Invalid gender parameter");
                return "redirect:/home";
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            List<ProfileCardDto> profiles = userService.getUserProfiles(gender, pageable);
            Map<UUID, Boolean> likedProfiles = new HashMap<>();
            for (ProfileCardDto profile : profiles) {
                likedProfiles.put(profile.getUserId(), matchingService.hasLiked(currentUser.getId(), profile.getUserId()));
            }

            model.addAttribute("profiles", profiles);
            model.addAttribute("likedProfiles", likedProfiles);
            model.addAttribute("gender", gender);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            return "user/gallery";
        } catch (MeetrulyException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/home";
        }
    }
}