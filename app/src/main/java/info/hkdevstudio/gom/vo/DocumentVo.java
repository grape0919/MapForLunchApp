package info.hkdevstudio.gom.vo;

import java.util.List;

/**
 * {
 *   "meta": {
 *     "same_name": {
 *       "region": [],
 *       "keyword": "카카오프렌즈",
 *       "selected_region": ""
 *     },
 *     "pageable_count": 14,
 *     "total_count": 14,
 *     "is_end": true
 *   },
 *   "documents": [
 *     {
 *       "place_name": "카카오프렌즈 코엑스점",
 *       "distance": "418",
 *       "place_url": "http://place.map.kakao.com/26338954",
 *       "category_name": "가정,생활 > 문구,사무용품 > 디자인문구 > 카카오프렌즈",
 *       "address_name": "서울 강남구 삼성동 159",
 *       "road_address_name": "서울 강남구 영동대로 513",
 *       "id": "26338954",
 *       "phone": "02-6002-1880",
 *       "category_group_code": "",
 *       "category_group_name": "",
 *       "x": "127.05902969025047",
 *       "y": "37.51207412593136"
 *     },
 *     ...
 *   ]
 * }
 */
public class DocumentVo {

    private String place_name;
    private String distance;
    private String place_url;
    private String category_name;
    private String address_name;
    private String road_address_name;
    private String id;
    private String phone;
    private String category_group_code;
    private String category_group_name;
    private String x;
    private String y;

    public String getPlace_name() {
        return place_name;
    }

    public void setPlace_name(String place_name) {
        this.place_name = place_name;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getPlace_url() {
        return place_url;
    }

    public void setPlace_url(String place_url) {
        this.place_url = place_url;
    }

    public String getCategory_name() {
        return category_name;
    }

    public void setCategory_name(String category_name) {
        this.category_name = category_name;
    }

    public String getAddress_name() {
        return address_name;
    }

    public void setAddress_name(String address_name) {
        this.address_name = address_name;
    }

    public String getRoad_address_name() {
        return road_address_name;
    }

    public void setRoad_address_name(String road_address_name) {
        this.road_address_name = road_address_name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCategory_group_code() {
        return category_group_code;
    }

    public void setCategory_group_code(String category_group_code) {
        this.category_group_code = category_group_code;
    }

    public String getCategory_group_name() {
        return category_group_name;
    }

    public void setCategory_group_name(String category_group_name) {
        this.category_group_name = category_group_name;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "DocumentVo{" +
                "place_name='" + place_name + '\'' +
                ", distance='" + distance + '\'' +
                ", place_url='" + place_url + '\'' +
                ", category_name='" + category_name + '\'' +
                ", address_name='" + address_name + '\'' +
                ", road_address_name='" + road_address_name + '\'' +
                ", id='" + id + '\'' +
                ", phone='" + phone + '\'' +
                ", category_group_code='" + category_group_code + '\'' +
                ", category_group_name='" + category_group_name + '\'' +
                ", x='" + x + '\'' +
                ", y='" + y + '\'' +
                '}';
    }
}
