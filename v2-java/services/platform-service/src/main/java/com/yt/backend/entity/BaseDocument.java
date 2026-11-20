package com.yt.backend.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * Lop cha cua moi entity. Document nao cung co `_id` (ObjectId) do MongoDB tu sinh,
 * nhung app khong dung toi no: khoa cua tung ban ghi la field `id` (chuoi UUID) dung chung voi ban Node.
 * Vi Spring Data coi thuoc tinh ten `id` la `_id`, `_id` duoc dat ten rieng la mongoId (@Id),
 * con field `id` cua tung entity khai bao bang @Field("id").
 */
@Getter
@Setter
public abstract class BaseDocument {

    @Id
    private String mongoId;
}
