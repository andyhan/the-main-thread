package academy.themainthread.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "partner_badge_template")
public class PartnerBadgeTemplate extends PanacheEntityBase {

    @EmbeddedId
    public PartnerBadgeTemplateId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("partnerId")
    @JoinColumn(name = "partner_id")
    public AccreditedPartner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_template_id", nullable = false)
    public BadgeTemplate template;

    public static PartnerBadgeTemplate findByCourseId(AccreditedPartner partner, String courseId) {
        return find("id.partnerId = ?1 AND id.courseId = ?2", partner.id, courseId).firstResult();
    }
}