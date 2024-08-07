package com.linked.classbridge.service;

import static com.linked.classbridge.type.ErrorCode.CLASS_NOT_FOUND;
import static com.linked.classbridge.type.ErrorCode.USER_NOT_FOUND;
import static com.linked.classbridge.type.ImageUpdateAction.ADD;

import com.linked.classbridge.domain.Lesson;
import com.linked.classbridge.domain.OneDayClass;
import com.linked.classbridge.domain.Review;
import com.linked.classbridge.domain.ReviewImage;
import com.linked.classbridge.domain.User;
import com.linked.classbridge.domain.document.OneDayClassDocument;
import com.linked.classbridge.dto.review.DeleteReviewResponse;
import com.linked.classbridge.dto.review.GetReviewResponse;
import com.linked.classbridge.dto.review.RegisterReviewDto;
import com.linked.classbridge.dto.review.RegisterReviewDto.Request;
import com.linked.classbridge.dto.review.UpdateReviewDto;
import com.linked.classbridge.dto.review.UpdateReviewImageDto;
import com.linked.classbridge.exception.RestApiException;
import com.linked.classbridge.repository.OneDayClassDocumentRepository;
import com.linked.classbridge.repository.ReviewImageRepository;
import com.linked.classbridge.repository.ReviewRepository;
import com.linked.classbridge.repository.UserRepository;
import com.linked.classbridge.type.ErrorCode;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final LessonService lessonService;
    private final S3Service s3Service;
    private final OneDayClassService classService;
    private final UserRepository userRepository;
    private final ElasticsearchOperations operations;
    private final OneDayClassDocumentRepository oneDayClassDocumentRepository;

    /**
     * 리뷰 등록
     *
     * @param request 리뷰 등록 요청
     * @return 리뷰 등록 응답
     */
    @Transactional
    public RegisterReviewDto.Response registerReview(User user, RegisterReviewDto.Request request,
                                                     MultipartFile[] reviewImages) {
        validateRegisterReview(request);

        Lesson lesson = lessonService.findLessonById(request.lessonId());
        OneDayClass oneDayClass = lesson.getOneDayClass();

        if (ObjectUtils.notEqual(request.classId(), oneDayClass.getClassId())) {
            throw new RestApiException(ErrorCode.INVALID_ONE_DAY_CLASS_ID);
        }

        validateReviewAlreadyExists(user, lesson);

        Review savedReview =
                reviewRepository.save(Request.toEntity(user, lesson, oneDayClass, request));

        uploadAndSaveReviewImage(savedReview, reviewImages);

        oneDayClass.addReview(savedReview);

        updateOneDayClassDocumentStarRate(oneDayClass);

        return RegisterReviewDto.Response.fromEntity(savedReview);
    }

    /**
     * 리뷰 수정
     *
     * @param user     사용자
     * @param request  수정할 리뷰 정보
     * @param reviewId 리뷰 ID
     * @return 수정된 리뷰 응답
     */
    @Transactional
    public UpdateReviewDto.Response updateReview(User user, UpdateReviewDto.Request request, Long reviewId) {
        validateUpdateReview(request);

        Review review = findReviewById(reviewId);
        OneDayClass oneDayClass = review.getOneDayClass();

        validateReviewOwner(user, review);

        Double prevRating = review.getRating();
        Double diffRating = request.rating() - prevRating; // 평점 차이 계산

        review.update(request.contents(), request.rating());

        oneDayClass.addStartRateDiff(diffRating); // 평점 업데이트

        updateOneDayClassDocumentStarRate(oneDayClass);

        return UpdateReviewDto.Response.fromEntity(review);
    }

    @Transactional
    public DeleteReviewResponse deleteReview(User user, Long reviewId) {

        Review review = findReviewById(reviewId);

        validateReviewOwner(user, review);

        List<ReviewImage> reviewImages =
                reviewImageRepository.findByReviewOrderBySequenceAsc(review);

        reviewImages.forEach(reviewImage -> s3Service.delete(reviewImage.getUrl()));

        review.getOneDayClass().removeReview(review);

        updateOneDayClassDocumentStarRate(review.getOneDayClass());

        reviewRepository.delete(review);

        return new DeleteReviewResponse(reviewId);
    }

    private void validateReviewAlreadyExists(User user, Lesson lesson) {
        reviewRepository.findByLessonAndUser(lesson, user).ifPresent(review -> {
            throw new RestApiException(ErrorCode.REVIEW_ALREADY_EXISTS);
        });
    }

    public void validateRegisterReview(RegisterReviewDto.Request request) {
        validateReviewRating(request.rating());
        validateReviewContents(request.contents());
    }

    private void validateReviewRating(Double rating) {
        if (rating < 0 || rating > 5) {
            throw new RestApiException(ErrorCode.INVALID_REVIEW_RATING);
        }
    }

    private void validateReviewContents(String contents) {
        if (contents.length() < 10 || contents.length() > 200) {
            throw new RestApiException(ErrorCode.INVALID_REVIEW_CONTENTS);
        }
    }

    private void uploadAndSaveReviewImage(Review savedReview, MultipartFile[] images) {
        // 리뷰 이미지 등록
        int sequence = 1;
        for (MultipartFile image : images) {
            String url = s3Service.uploadReviewImage(image);
            reviewImageRepository.save(ReviewImage.builder()
                    .review(savedReview)
                    .url(url)
                    .sequence(sequence++)
                    .build());
        }
    }


    public void validateUpdateReview(UpdateReviewDto.Request request) {
        validateReviewRating(request.rating());
        validateReviewContents(request.contents());
    }

    private void validateReviewOwner(User user, Review review) {
        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new RestApiException(ErrorCode.NOT_REVIEW_OWNER);
        }
    }

    public Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RestApiException(ErrorCode.REVIEW_NOT_FOUND));
    }


    @Transactional
    public void updateReviewImages(User user, Long reviewId,
                                   List<UpdateReviewImageDto> updateReviewImageDtoList,
                                   MultipartFile[] reviewImages) {
        Review review = findReviewById(reviewId);

        validateReviewOwner(user, review);

        List<ReviewImage> reviewImagesList = reviewImageRepository.findByReviewOrderBySequenceAsc(review);

        for (UpdateReviewImageDto updateReviewImageDto : updateReviewImageDtoList) {
            ReviewImage reviewImage = updateReviewImageDto.getAction() == ADD ?
                    null :
                    reviewImagesList.stream()
                            .filter(image ->
                                    Objects.equals(image.getReviewImageId(), updateReviewImageDto.getImageId()))
                            .findFirst()
                            .orElseThrow(() -> new RestApiException(ErrorCode.REVIEW_IMAGE_NOT_FOUND));

            switch (updateReviewImageDto.getAction()) {
                case KEEP -> {
                    reviewImage.setSequence(updateReviewImageDto.getSequence());
                }
                case ADD -> {
                    String url = s3Service.uploadReviewImage(reviewImages[updateReviewImageDto.getSequence() - 1]);
                    reviewImage = ReviewImage.builder()
                            .review(review)
                            .url(url)
                            .sequence(updateReviewImageDto.getSequence())
                            .build();
                    reviewImageRepository.save(reviewImage);
                }
                case DELETE -> {
                    s3Service.delete(reviewImage.getUrl());
                    reviewImageRepository.delete(reviewImage);
                }
                case REPLACE -> {
                    s3Service.delete(reviewImage.getUrl());
                    String newUrl = s3Service.uploadReviewImage(reviewImages[updateReviewImageDto.getSequence() - 1]);
                    reviewImage.updateUrl(newUrl);
                    reviewImage.setSequence(updateReviewImageDto.getSequence());
                }
                default -> throw new RestApiException(ErrorCode.INVALID_REVIEW_IMAGE_ACTION);
            }
        }
    }

    /**
     * 리뷰 조회
     *
     * @param reviewId 리뷰 ID
     * @return 리뷰 응답
     */
    public GetReviewResponse getReview(Long reviewId) {
        Review review = findReviewById(reviewId);
        return GetReviewResponse.fromEntity(review);
    }

    /**
     * 클래스 리뷰 조회
     *
     * @param classId  클래스 ID
     * @param pageable 페이징 정보
     * @return 리뷰 응답
     */
    public Page<GetReviewResponse> getClassReviews(Long classId, Pageable pageable) {

        OneDayClass oneDayClass = classService.findClassById(classId);

        Page<Review> reviews = reviewRepository.findByOneDayClass(oneDayClass, pageable);

        return reviews.map(GetReviewResponse::fromEntity);
    }

    /**
     * 사용자 리뷰 조회
     *
     * @param user     사용자
     * @param pageable 페이징 정보
     * @return 리뷰 응답
     */
    public Page<GetReviewResponse> getUserReviews(User user, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUser(user, pageable);
        return reviews.map(GetReviewResponse::fromEntity);
    }

    /**
     * 강사 리뷰 조회
     *
     * @param email    강사
     * @param pageable 페이징 정보
     * @return 리뷰 응답
     */

    public Page<GetReviewResponse> getTutorReviews(String email, Pageable pageable) {
        User tutor = userRepository.findByEmail(email).orElseThrow(() -> new RestApiException(USER_NOT_FOUND));
        Page<Review> reviews = reviewRepository.findByTutor(tutor, pageable);
        return reviews.map(GetReviewResponse::fromEntity);
    }

    private void updateOneDayClassDocumentStarRate(OneDayClass oneDayClass) {
        OneDayClassDocument oneDayClassDocument = oneDayClassDocumentRepository.findById(oneDayClass.getClassId())
                .orElseThrow(() -> new RestApiException(CLASS_NOT_FOUND));
        oneDayClassDocument.setStarRate(oneDayClass.getTotalStarRate() / (double)(oneDayClass.getTotalReviews() == 0 ? 1
                : oneDayClass.getTotalReviews()));
        oneDayClassDocument.setTotalReviews(oneDayClass.getTotalReviews());
        operations.save(oneDayClassDocument);
    }
}
