package com.luixtech.luixrpc.democommon.service;

import com.luixtech.luixrpc.democommon.domain.Dict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface DictService {

    Page<Dict> find(Pageable pageable, String dictName, Boolean enabled);

    Map<String, String> findDictCodeDictNameMap();

}