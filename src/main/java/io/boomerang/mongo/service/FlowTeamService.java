package io.boomerang.mongo.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.mongo.entity.TeamEntity;

public interface FlowTeamService {

  Page<TeamEntity> findAllTeams(Pageable pageable);
  
  Page<TeamEntity> findAllActiveTeams(Pageable pageable);

  List<TeamEntity> findTeamsWithHighLevelGroups(List<String> highLevelGroups);

  TeamEntity save(TeamEntity entity);

  TeamEntity findById(String id);

}
