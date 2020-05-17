package info.hkdevstudio.gom.vo;
/**
 * {
 *   "meta" = {
 *     "same_name" = {
 *       "region" = [],
 *       "keyword" = "카카오프렌즈",
 *       "selected_region" = ""
 *     },
 *     "pageable_count" = 14,
 *     "total_count" = 14,
 *     "is_end" = true
 *   },
 *   
 */
public class MetaVo {
    private int pageable_count;
    private int total_count;
    private boolean is_end;

    public int getPageable_count() {
        return pageable_count;
    }

    public void setPageable_count(int pageable_count) {
        this.pageable_count = pageable_count;
    }

    public int getTotal_count() {
        return total_count;
    }

    public void setTotal_count(int total_count) {
        this.total_count = total_count;
    }

    public boolean getIs_end() {
        return is_end;
    }

    public void setIs_end(boolean is_end) {
        this.is_end = is_end;
    }
}
