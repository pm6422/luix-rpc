package com.luixtech.rpc.democommon.service;

import com.luixtech.rpc.democommon.domain.DictItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DictItemService {

    DictItem insert(DictItem domain);

    void update(DictItem domain);

    Page<DictItem> find(Pageable pageable, String dictCode, String dictItemName);
}