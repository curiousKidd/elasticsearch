package my.project.elk.service.impl;

import my.project.elk.service.ElkService;
import org.springframework.stereotype.Service;

@Service("elkService")
public class ElkServiceImpl implements ElkService {

    @Override
    public void getList() {
        // TODO: List 가져오는 방법 추가하기 _ elasticsearch 8.3.2
    }
}
