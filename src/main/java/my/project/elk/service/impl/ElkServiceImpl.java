package my.project.elk.service.impl;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import my.project.elk.index.Index;
import my.project.elk.service.ElkService;
import org.springframework.stereotype.Service;

@Service("elkService")
@RequiredArgsConstructor
@Log4j2
public class ElkServiceImpl implements ElkService {

    private final Index index;

    @Override
    public void getList(String Keyword) {
        // TODO: List 가져오는 방법 추가하기 _ elasticsearch 8.3.2
        try {
            SearchResponse<Object> searchResponse = null;
            //            List<DTO> hits = new ArrayList<>();

            String indexLast = index.getIndex();

            // TODO: 2022/09/08 index value search
            //searchResponse = getSearchResponse(keyword, indexLast);
            //            HitsMetadata<Object> searchHits = searchResponse.hits();

            //            if (searchHits.maxScore() != null && searchHits.total().value() > 0) {
            //                for (Hit<Object> hit : searchHits.hits()) {
            //                    Map<String, Object> hitMap = (Map<String, Object>) hit.source();
            //                    Double score = hit.score();
            //                    Map<String, List<String>> highlightMap = hit.highlight();

            //                    BaygleSellSearchInnerDTO baygleHit = BaygleSellSearchInnerDTO.Builder.BaygleSellSearchInnerDTO()
            //                            .iitemseq((Long) hitMap.get("iitemseq"))
            //                            .caddtype(StringUtil.objectIsNullByString(hitMap.get("caddtype")))
            //                            .biwantquantity(((Number) hitMap.get("biwantquantity")).longValue())
            //                            .vcgamenamengramhighlight(highlightMap.get("vcgamenamengram") != null ? highlightMap.get("vcgamenamengram").get(0) : (String) hitMap.get("vcgamenamengram"))
            //                            .vcservernamengramhighlight(highlightMap.get("vcservernamengram") != null ? highlightMap.get("vcservernamengram").get(0) : (String) hitMap.get("vcservernamengram"))
            //
            //                            .score(score)
            //                    .build();

            //        ResponseDTO responseDTO = ResponseDTO
            //                    .totalHits(searchHits.total().value())
            //                    .hits(baygleHits)
            //                    .build();

        } catch (Exception e) {
            log.error("Error", e);
            throw new RuntimeException("Error : " + e.getMessage());
        }
    }
}
