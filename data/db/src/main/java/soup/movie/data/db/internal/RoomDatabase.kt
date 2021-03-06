package soup.movie.data.db.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import soup.movie.data.db.MoopDatabase
import soup.movie.data.db.internal.dao.FavoriteMovieDao
import soup.movie.data.db.internal.dao.MovieCacheDao
import soup.movie.data.db.internal.dao.OpenDateAlarmDao
import soup.movie.data.db.internal.entity.MovieListEntity
import soup.movie.data.db.internal.entity.MovieListEntity.Companion.TYPE_NOW
import soup.movie.data.db.internal.entity.MovieListEntity.Companion.TYPE_PLAN
import soup.movie.data.db.internal.mapper.*
import soup.movie.model.Movie
import soup.movie.model.MovieList
import soup.movie.model.OpenDateAlarm
import soup.movie.model.TheaterAreaGroup

internal class RoomDatabase(
    private val favoriteMovieDao: FavoriteMovieDao,
    private val openDateAlarmDao: OpenDateAlarmDao,
    private val cacheDao: MovieCacheDao
) : MoopDatabase {

    private var codeResponse: TheaterAreaGroup? = null

    override suspend fun saveNowMovieList(movieList: MovieList) {
        saveMovieListAs(TYPE_NOW, movieList)
        favoriteMovieDao.updateAll(movieList.list.map { it.toFavoriteMovieEntity() })
    }

    override fun getNowMovieListFlow(): Flow<List<Movie>> {
        return getMovieListFlow(TYPE_NOW)
    }

    override suspend fun savePlanMovieList(movieList: MovieList) {
        saveMovieListAs(TYPE_PLAN, movieList)
        favoriteMovieDao.updateAll(movieList.list.map { it.toFavoriteMovieEntity() })
        openDateAlarmDao.updateAll(movieList.list.map { it.toOpenDateAlarmEntity() })
    }

    override fun getPlanMovieListFlow(): Flow<List<Movie>> {
        return getMovieListFlow(TYPE_PLAN)
    }

    private suspend fun saveMovieListAs(type: String, movieList: MovieList) {
        cacheDao.insert(
            MovieListEntity(
                type,
                movieList.lastUpdateTime,
                movieList.list.map { it.toMovieEntity() }
            )
        )
    }

    private fun getMovieListFlow(type: String): Flow<List<Movie>> {
        return cacheDao.getMovieListByType(type)
            .map { it.list.map { movieEntity -> movieEntity.toMovie() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun getNowLastUpdateTime(): Long {
        return cacheDao.findByType(TYPE_NOW).lastUpdateTime
    }

    override suspend fun getPlanLastUpdateTime(): Long {
        return cacheDao.findByType(TYPE_PLAN).lastUpdateTime
    }

    override suspend fun getAllMovieList(): List<Movie> {
        return getNowMovieList() + getPlanMovieList()
    }

    override suspend fun getNowMovieList(): List<Movie> {
        return getMovieListOf(TYPE_NOW)
    }

    private suspend fun getPlanMovieList(): List<Movie> {
        return getMovieListOf(TYPE_PLAN)
    }

    private suspend fun getMovieListOf(type: String): List<Movie> {
        return try {
            cacheDao.findByType(type).list
                .map { movieEntity -> movieEntity.toMovie() }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    override fun saveCodeList(response: TheaterAreaGroup) {
        codeResponse = response
    }

    override fun getCodeList(): TheaterAreaGroup? {
        return codeResponse
    }

    override suspend fun addFavoriteMovie(movie: Movie) {
        favoriteMovieDao.insertFavoriteMovie(movie.toFavoriteMovieEntity())
    }

    override suspend fun removeFavoriteMovie(movieId: String) {
        favoriteMovieDao.deleteFavoriteMovie(movieId)
        openDateAlarmDao.delete(movieId)
    }

    override fun getFavoriteMovieList(): Flow<List<Movie>> {
        return favoriteMovieDao.getFavoriteMovieList().map {
            it.map { favoriteMovieEntity -> favoriteMovieEntity.toMovie() }
        }
    }

    override suspend fun isFavoriteMovie(movieId: String): Boolean {
        return favoriteMovieDao.isFavoriteMovie(movieId)
    }

    /**
     * @param date yyyy.mm.dd ex) 2020.01.31
     */
    override suspend fun getOpenDateAlarmListUntil(date: String): List<OpenDateAlarm> {
        return openDateAlarmDao.getAllUntil(date)
            .map { openDateAlarmEntity -> openDateAlarmEntity.toOpenDateAlarm() }
    }

    override suspend fun hasOpenDateAlarms(): Boolean {
        return openDateAlarmDao.hasAlarms()
    }

    override suspend fun insertOpenDateAlarm(alarm: OpenDateAlarm) {
        openDateAlarmDao.insert(alarm.toEntity())
    }

    override suspend fun deleteOpenDateAlarms(alarms: List<OpenDateAlarm>) {
        return openDateAlarmDao.deleteAll(alarms.map { it.movieId })
    }
}
