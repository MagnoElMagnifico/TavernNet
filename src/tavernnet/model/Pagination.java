package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.Document;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NullMarked
public record Pagination<T> (
    List<T> page,

    @JsonProperty("page_number")
    int pageNumber,

    @JsonProperty("total_count")
    int totalCount
) {
    public static Pagination<String> from(AggregationResults<Document> root, int pageNumber) {
        var realRoot = root.getMappedResults().getFirst();
        int count = 0;
        if (
            realRoot.get("total_count") instanceof List<?> list
            && !list.isEmpty()
            && list.getFirst() instanceof Map<?, ?> map
            && map.get("count") instanceof Integer c
        ) {
            count = c;
        }

        // Esto está bien, como mucho procesamos el límite maximo de elementos
        // permitidos por página, que es 1000.
        List<String> page = new ArrayList<>();
        if (realRoot.get("page_data") instanceof List<?> pageData) {
            for (Object obj : pageData) {
                if (obj instanceof Map<?, ?> map && map.get("_id") instanceof String username) {
                    page.add(username);
                }
            }
        }

        return new Pagination<>(page, pageNumber, count);
    }
}
