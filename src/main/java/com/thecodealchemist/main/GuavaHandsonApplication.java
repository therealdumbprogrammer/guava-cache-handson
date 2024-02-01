package com.thecodealchemist.main;

import com.google.common.cache.*;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class GuavaHandsonApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuavaHandsonApplication.class, args);
	}

	@RestController
	class ReadController {
		@Autowired
		private CacheService cacheService;

		@GetMapping("/movie/{director}")
		public String movieByDirector(@PathVariable String director) throws ExecutionException {
			return cacheService.getCache().get(director);
		}
	}

	@Service
	class CacheService {
		private LoadingCache<String, String> movieCache;

		@Autowired
		private DataDao dao;

		public CacheService() {

			movieCache = CacheBuilder.newBuilder()
					//.maximumSize(1)
					//.expireAfterAccess(Duration.ofSeconds(10))
					.maximumWeight(10)
					.weigher((Weigher<String, String>) (key, value) -> {
                        System.out.println("Calculating the weight for key = " + key +", len = " + value.length());
                        return value.length()/2;
                    })
					.removalListener(new RemovalListener<String, String>() {
						@Override
						public void onRemoval(RemovalNotification<String, String> notification) {
							System.out.println(notification.toString() +", " + notification.getCause().toString());
						}
					})
					.build(
					new CacheLoader<String, String>() {
						@Override
						public String load(String key) throws Exception {
							System.out.println("Calling cacheloader::" + key);
							return dao.movieByDirector(key);
						}

						@Override
						public Map<String, String> loadAll(Iterable<? extends String> keys) throws Exception {
							System.out.println("Calling cacheloader -> loadAll::" + keys);
							Map<String, String> map = new HashMap<>();
							for(String key : keys) {
								map.put(key, dao.movieByDirector(key));
							}
							return map;
						}
					}
			);
		}

		public LoadingCache<String, String> getCache() {
			return movieCache;
		}
	}

	@Repository
	class DataDao {
		private static final Map<String, String> DATA = Map.of(
				"John","Die Hard","George","Mad Max: Fury Road",
				"Chad","John Wick",
				"James","Terminator 2: Judgment Day",
				"The Wachowskis","The Matrix","Richard","Lethal Weapon",
				"Christopher","Mission: Impossible - Fallout",
				"Quentin","Kill Bill: Volume 1","Ridley","Gladiator"
		);

		Map<String, String> getAll() {
			System.out.println("CacheMiss -> DAO -> Calling getAll");
			return DATA;
		}

		String movieByDirector(String director) {
			System.out.println("CacheMiss -> DAO -> Calling movieByDirector -> " + director);
			return DATA.get(director);
		}
	}

}
