package org.infinity.rpc.appclient.service.impl;

import org.infinity.rpc.appclient.domain.Dict;
import org.infinity.rpc.appclient.repository.DictRepository;
import org.infinity.rpc.appclient.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private DictRepository dictRepository;

    @Override
    public Map<String, String> findDictCodeDictNameMap() {
        return dictRepository.findAll().stream().collect(Collectors.toMap(Dict::getDictCode, Dict::getDictName));
    }
}