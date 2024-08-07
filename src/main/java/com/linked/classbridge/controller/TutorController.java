package com.linked.classbridge.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.linked.classbridge.dto.SuccessResponse;
import com.linked.classbridge.dto.oneDayClass.ClassDto;
import com.linked.classbridge.dto.oneDayClass.ClassDto.ClassResponseByTutor;
import com.linked.classbridge.dto.oneDayClass.ClassFAQDto;
import com.linked.classbridge.dto.oneDayClass.ClassTagDto;
import com.linked.classbridge.dto.oneDayClass.ClassUpdateDto;
import com.linked.classbridge.dto.oneDayClass.LessonDtoDetail;
import com.linked.classbridge.dto.review.GetReviewResponse;
import com.linked.classbridge.dto.tutor.TutorInfoDto;
import com.linked.classbridge.service.OneDayClassService;
import com.linked.classbridge.service.ReviewService;
import com.linked.classbridge.service.TutorService;
import com.linked.classbridge.service.UserService;
import com.linked.classbridge.type.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tutors")
public class TutorController {

    private final ReviewService reviewService;
    private final OneDayClassService oneDayClassService;
    private final UserService userService;
    private final TutorService tutorService;

    @Operation(summary = "강사 등록", description = "강사 등록 및 강사 세부 정보 업로드")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<String>> registerTutor(@RequestBody @Valid TutorInfoDto tutorInfoDto) {

        return ResponseEntity.ok().body(
                SuccessResponse.of(
                        ResponseMessage.TUTOR_REGISTER_SUCCESS,
                        tutorService.registerTutor(tutorInfoDto)
                )
        );
    }

    @Operation(summary = "강사 정보 수정", description = "강사 정보 수정")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/update")
    public ResponseEntity<SuccessResponse<String>> updateTutorInfo(@RequestBody @Valid TutorInfoDto tutorInfoDto) {

        return ResponseEntity.ok().body(
                SuccessResponse.of(
                        ResponseMessage.TUTOR_UPDATE_SUCCESS,
                        tutorService.updateTutorInfo(tutorInfoDto)
                )
        );
    }

    @Operation(summary = "강사 리뷰 조회", description = "강사 리뷰 조회")
    @GetMapping("/reviews")
    public ResponseEntity<SuccessResponse<Page<GetReviewResponse>>> getClassReviews(
            @PageableDefault Pageable pageable
    ) {
        return ResponseEntity.ok().body(
                SuccessResponse.of(
                        ResponseMessage.REVIEW_GET_SUCCESS,
                        reviewService.getTutorReviews(userService.getCurrentUserEmail(), pageable)
                )
        );
    }

    /**
     * 강사 클래스 리스트 조회
     * @param pageable
     * @return ResponseEntity<SuccessResponse<Page<ClassDto>>
     */
    @Operation(summary = "Class list 조회", description = "Class list 조회")
    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/class")
    public ResponseEntity<SuccessResponse<Page<ClassDto>>> getOneDayClassList(Pageable pageable) {
        return ResponseEntity.status(OK).body(SuccessResponse.of(
                ResponseMessage.ONE_DAY_CLASS_LIST_GET_SUCCESS,
                oneDayClassService.getOneDayClassList(userService.getCurrentUserEmail(), pageable))
        );
    }

    /**
     * 강사 클래스 조회
     * @param
     * @return ResponseEntity<SuccessResponse<ClassDto.Response>
     */
    @Operation(summary = "Class 조회", description = "Class 조회")
    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/class/{classId}")
    public ResponseEntity<SuccessResponse<ClassResponseByTutor>> getOneDayClass(
            @PathVariable Long classId) {
        return ResponseEntity.status(OK).body(SuccessResponse.of(
                ResponseMessage.ONE_DAY_CLASS_GET_SUCCESS,
                oneDayClassService.getOneDayClassByTutor(userService.getCurrentUserEmail(), classId))
        );
    }

    /**
     * Class 등록
     * @param   request, file1, file2, file3, file4, file5
     * @return  ResponseEntity<SuccessResponse<ClassDto>>
     */
    @Operation(summary = "Class 등록", description = "Class 등록")
    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping(path = "/class", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponse<ClassResponseByTutor>> registerClass(
            @RequestPart(value = "request") @Valid ClassDto.ClassRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images

    ) {

        return ResponseEntity.status(CREATED).body(SuccessResponse.of(
                ResponseMessage.CLASS_REGISTER_SUCCESS,
                oneDayClassService.registerClass(userService.getCurrentUserEmail(), request, images))
        );
    }

    /**
     * Class 세부 정보 수정
     * @param   request
     * @return  ResponseEntity<SuccessResponse<ClassUpdateDto.ClassResponse>>
     */
    @Operation(summary = "Class 세부 정보 수정", description = "Class 세부 정보 수정")
    @PreAuthorize("hasRole('TUTOR')")
    @PutMapping(path = "/class/{classId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE )
    public ResponseEntity<SuccessResponse<ClassUpdateDto.ClassResponse>> updateClass(
            @PathVariable Long classId,
            @RequestPart(value = "request") @Valid ClassUpdateDto.ClassRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] files
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_UPDATE_SUCCESS,
                oneDayClassService.updateClass(userService.getCurrentUserEmail(), request, files, classId))
        );
    }

    /**
     * Class 삭제
     * @param   classId
     * @return  ResponseEntity<SuccessResponse<ClassDto>>
     */
    @Operation(summary = "Class 삭제", description = "Class 삭제")
    @PreAuthorize("hasRole('TUTOR')")
    @DeleteMapping(path = "/class/{classId}")
    public ResponseEntity<SuccessResponse<Boolean>> deleteClass(
            @PathVariable Long classId
    ) {
        return ResponseEntity.status(OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_DELETE_SUCCESS,
                oneDayClassService.deleteClass(userService.getCurrentUserEmail(), classId))
        );
    }

    /**
     * Class FAQ 추가
     * @param   request
     * @return  ResponseEntity<SuccessResponse<ClassDto>>
     */
    @Operation(summary = "Class FAQ 추가", description = "Class FAQ 추가")
    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping(path = "/class/{classId}/faqs")
    public ResponseEntity<SuccessResponse<ClassFAQDto>> registerFAQ(
            @RequestBody @Valid ClassFAQDto request,
            @PathVariable Long classId
    ) {
        return ResponseEntity.status(CREATED).body(SuccessResponse.of(
                ResponseMessage.CLASS_FAQ_REGISTER_SUCCESS,
                oneDayClassService.registerFAQ(userService.getCurrentUserEmail(), request, classId))
        );
    }

    /**
     * Class FAQ 수정
     * @param   request, classId, faqId
     * @return  ResponseEntity<SuccessResponse<ClassFAQDto>>
     */
    @Operation(summary = "Class FAQ 수정", description = "Class FAQ 수정")
    @PreAuthorize("hasRole('TUTOR')")
    @PutMapping(path = "/class/{classId}/faqs/{faqId}")
    public ResponseEntity<SuccessResponse<ClassFAQDto>> updateFAQ(
            @RequestBody @Valid ClassFAQDto request,
            @PathVariable Long classId,
            @PathVariable Long faqId
    ) {
        return ResponseEntity.status(OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_FAQ_UPDATE_SUCCESS,
                oneDayClassService.updateFAQ(userService.getCurrentUserEmail(), request, classId, faqId))
        );
    }

    /**
     * Class FAQ 삭제
     * @param   classId, faqId
     * @return  ResponseEntity<SuccessResponse<ClassFAQDto>>
     */
    @Operation(summary = "Class FAQ 삭제", description = "Class FAQ 삭제")
    @PreAuthorize("hasRole('TUTOR')")
    @DeleteMapping(path = "/class/{classId}/faqs/{faqId}")
    public ResponseEntity<SuccessResponse<Boolean>> deleteFAQ(
            @PathVariable Long classId,
            @PathVariable Long faqId
    ) {
        return ResponseEntity.status(OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_FAQ_DELETE_SUCCESS,
                oneDayClassService.deleteFAQ(userService.getCurrentUserEmail(), classId, faqId))
        );
    }

    /**
     * Class lesson 추가
     * @param   request
     * @return  ResponseEntity<SuccessResponse<ClassDto>>
     */
    @Operation(summary = "Class lesson 추가", description = "Class lesson 추가")
    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping(path = "/class/{classId}/lessons")
    public ResponseEntity<SuccessResponse<LessonDtoDetail>> registerFAQ(
            @RequestBody @Valid LessonDtoDetail.Request request,
            @PathVariable Long classId
    ) {
        return ResponseEntity.status(CREATED).body(SuccessResponse.of(
                ResponseMessage.CLASS_LESSON_REGISTER_SUCCESS,
                oneDayClassService.registerLesson(userService.getCurrentUserEmail(), request, classId))
        );
    }

    /**
     * Class lesson 수정
     * @param   classId, lessonId
     * @return  ResponseEntity<SuccessResponse<Boolean>>
     */
    @Operation(summary = "Class lesson 수정", description = "Class lesson 수정")
    @PreAuthorize("hasRole('TUTOR')")
    @PutMapping(path = "/class/{classId}/lessons/{lessonId}")
    public ResponseEntity<SuccessResponse<LessonDtoDetail>> updateLesson(
            @PathVariable Long classId,
            @PathVariable Long lessonId,
            @RequestBody LessonDtoDetail.Request request
    ) {
        return ResponseEntity.status(OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_LESSON_UPDATE_SUCCESS,
                oneDayClassService.updateLesson(userService.getCurrentUserEmail(), request, classId, lessonId))
        );
    }

    /**
     * Class lesson 삭제
     * @param   classId, lessonId
     * @return  ResponseEntity<SuccessResponse<Boolean>>
     */
    @Operation(summary = "Class lesson 삭제", description = "Class lesson 삭제")
    @PreAuthorize("hasRole('TUTOR')")
    @DeleteMapping(path = "/class/{classId}/lessons/{lessonId}")
    public ResponseEntity<SuccessResponse<Boolean>> deleteLesson(
            @PathVariable Long classId,
            @PathVariable Long lessonId
    ) {
        return ResponseEntity.status(OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_LESSON_DELETE_SUCCESS,
                oneDayClassService.deleteLesson(userService.getCurrentUserEmail(), classId, lessonId))
        );
    }

    /**
     * Class tag 추가
     * @param   request
     * @return  ResponseEntity<SuccessResponse<ClassTagDto>>
     */
    @Operation(summary = "Class tag 추가", description = "Class tag 추가")
    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping(path = "/class/{classId}/tags")
    public ResponseEntity<SuccessResponse<ClassTagDto>> registerTag(
            @PathVariable Long classId,
            @RequestBody @Valid ClassTagDto request) {
        return ResponseEntity.status(CREATED).body(SuccessResponse.of(
                ResponseMessage.CLASS_TAG_REGISTER_SUCCESS,
                oneDayClassService.registerTag(userService.getCurrentUserEmail(), request, classId))
        );
    }

    /**
     * Class tag 수정
     * @param   request
     * @return  ResponseEntity<SuccessResponse<ClassTagDto>>
     */
    @Operation(summary = "Class tag 수정", description = "Class tag 수정")
    @PreAuthorize("hasRole('TUTOR')")
    @PutMapping(path = "/class/{classId}/tags/{tagId}")
    public ResponseEntity<SuccessResponse<ClassTagDto>> updateTag(
            @PathVariable Long classId,
            @RequestBody @Valid ClassTagDto request,
            @PathVariable Long tagId) {
        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_TAG_UPDATE_SUCCESS,
                oneDayClassService.updateTag(userService.getCurrentUserEmail(), request, classId, tagId))
        );
    }

    /**
     * Class tag 삭제
     * @param
     * @return  ResponseEntity<SuccessResponse<Boolean>>
     */
    @Operation(summary = "Class tag 삭제", description = "Class tag 삭제")
    @PreAuthorize("hasRole('TUTOR')")
    @DeleteMapping(path = "/class/{classId}/tags/{tagId}")
    public ResponseEntity<SuccessResponse<Boolean>> deleteTag(
            @PathVariable Long classId,
            @PathVariable Long tagId ) {

        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.of(
                ResponseMessage.CLASS_TAG_DELETE_SUCCESS,
                oneDayClassService.deleteTag(userService.getCurrentUserEmail(), classId, tagId))
        );
    }

    @Operation(summary = "출석체크", description = "예약자에 대한 출석체크를 진행")
    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping("/check-attendance")
    public ResponseEntity<SuccessResponse<String>> checkAttendance(
            @RequestParam Long userId, @RequestParam Long reservationId) {

        return ResponseEntity.ok().body(
                SuccessResponse.of(
                        ResponseMessage.ATTENDANCE_CHECK_SUCCESS,
                        tutorService.checkAttendance(userId, reservationId)
                )
        );
    }
}
