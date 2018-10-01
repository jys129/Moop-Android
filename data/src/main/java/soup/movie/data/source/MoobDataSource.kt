package soup.movie.data.source

import io.reactivex.Observable
import soup.movie.data.model.Movie
import soup.movie.data.model.Theater
import soup.movie.data.model.Version
import soup.movie.data.model.response.CodeResponse
import soup.movie.data.model.response.MovieListResponse
import soup.movie.data.model.response.TimetableResponse

interface MoobDataSource {

    fun getNowList(): Observable<MovieListResponse>

    fun getPlanList(): Observable<MovieListResponse>

    fun getCodeList(): Observable<CodeResponse>

    fun getTimetable(theater: Theater, movie: Movie): Observable<TimetableResponse>

    fun getVersion(pkgName: String, defaultVersion: String): Observable<Version>
}
