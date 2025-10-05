package com.alotra.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CTDonHangToppingId implements Serializable {
    private Integer ctId;
    private Integer toppingId;

    public CTDonHangToppingId() {}
    public CTDonHangToppingId(Integer ctId, Integer toppingId) {
        this.ctId = ctId;
        this.toppingId = toppingId;
    }

    public Integer getCtId() { return ctId; }
    public void setCtId(Integer ctId) { this.ctId = ctId; }
    public Integer getToppingId() { return toppingId; }
    public void setToppingId(Integer toppingId) { this.toppingId = toppingId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CTDonHangToppingId that)) return false;
        return Objects.equals(ctId, that.ctId) && Objects.equals(toppingId, that.toppingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctId, toppingId);
    }
}
