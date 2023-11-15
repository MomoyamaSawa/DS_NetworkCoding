package service.info.infoservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import service.info.infoservice.pojo.Data;

public interface DataRepository extends JpaRepository<Data, String> {
    List<Data> findByFileid(String fileid);
}