package com.alotra.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GioHangCTToppingId implements Serializable {
    private Integer ctghId;
    private Integer toppingId;

    public GioHangCTToppingId() {}
    public GioHangCTToppingId(Integer ctghId, Integer toppingId) {
        this.ctghId = ctghId;
        this.toppingId = toppingId;
    }

    public Integer getCtghId() { return ctghId; }
    public void setCtghId(Integer ctghId) { this.ctghId = ctghId; }
    public Integer getToppingId() { return toppingId; }
    public void setToppingId(Integer toppingId) { this.toppingId = toppingId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GioHangCTToppingId that)) return false;
        return Objects.equals(ctghId, that.ctghId) && Objects.equals(toppingId, that.toppingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctghId, toppingId);
    }
}
