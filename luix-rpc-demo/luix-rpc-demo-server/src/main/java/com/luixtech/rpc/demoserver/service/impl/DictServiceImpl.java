package com.luixtech.rpc.demoserver.service.impl;

import com.luixtech.rpc.core.server.annotation.RpcProvider;
import com.luixtech.rpc.democommon.domain.Dict;
import com.luixtech.rpc.democommon.service.DictService;
import com.luixtech.rpc.demoserver.repository.DictRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.stream.Collectors;

@RpcProvider
@AllArgsConstructor
public class DictServiceImpl implements DictService {
    private final DictRepository dictRepository;

    @Override
    public Page<Dict> find(Pageable pageable, String dictName, Boolean enabled) {
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Example<Dict> queryExample = Example.of(new Dict(dictName, enabled), matcher);
        return dictRepository.findAll(queryExample, pageable);
    }

    @Override
    public Map<String, String> findDictCodeDictNameMap() {
        return dictRepository.findAll().stream().collect(Collectors.toMap(Dict::getDictCode, Dict::getDictName));
    }
}