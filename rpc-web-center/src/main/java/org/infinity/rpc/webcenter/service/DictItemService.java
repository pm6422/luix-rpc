package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.webcenter.domain.DictItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DictItemService {

    DictItem insert(DictItem domain);

    void update(DictItem domain);

    Page<DictItem> find(Pageable pageable, String dictCode, String dictItemName);
}