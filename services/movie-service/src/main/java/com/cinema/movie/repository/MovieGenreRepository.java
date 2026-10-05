package com.cinema.movie.repository; // 3.1

import java.util.Collection; // 3.2
import java.util.List; // 3.3
import java.util.UUID; // 3.4

import org.springframework.data.jpa.repository.JpaRepository; // 3.5
import org.springframework.data.jpa.repository.Query; // 3.6
import org.springframework.data.repository.query.Param; // 3.7

import com.cinema.movie.entity.MovieGenreEntity; // 3.8

public interface MovieGenreRepository extends JpaRepository<MovieGenreEntity, MovieGenreEntity.Key> { // 3.9 Khóa kép = Key (đã có)

    interface MovieGenreName { // 3.10 Projection: chỉ lấy 2 cột cần
        UUID getMovieId(); // 3.11 Khớp alias "movieId" trong query
        String getName(); // 3.12 Khớp alias "name"
    }

    @Query("select mg.movieId as movieId, g.name as name " // 3.13 JPQL: movie_genres nối genres
            + "from MovieGenreEntity mg join GenreEntity g on g.id = mg.genreId " // 3.14 Nối hai entity theo điều kiện ON
            + "where mg.movieId in :movieIds order by g.name") // 3.15 Lọc theo danh sách phim, sắp theo tên thể loại
    List<MovieGenreName> findGenreNamesByMovieIds(@Param("movieIds") Collection<UUID> movieIds); // 3.16
}