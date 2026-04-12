package academy.themainthread.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PartnerBadgeTemplateId implements Serializable {

    @Column(name = "partner_id", nullable = false)
    public UUID partnerId;

    @Column(name = "course_id", nullable = false, length = 255)
    public String courseId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PartnerBadgeTemplateId that = (PartnerBadgeTemplateId) o;
        return Objects.equals(partnerId, that.partnerId) && Objects.equals(courseId, that.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partnerId, courseId);
    }
}