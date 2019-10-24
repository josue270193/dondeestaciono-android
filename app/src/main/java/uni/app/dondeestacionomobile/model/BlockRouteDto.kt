package uni.app.dondeestacionomobile.model

import org.threeten.bp.OffsetDateTime
import uni.app.dondeestacionomobile.model.enumerate.TypeRouteBlock

class BlockRouteDto {

    var id: String? = null
    var tweetData: TweetDataDto? = null
    var started: OffsetDateTime? = null
    var finished: OffsetDateTime? = null
    var direction: String? = null
    var type: TypeRouteBlock? = null
    var isTotal: Boolean? = null
    var point: PointDto? = null
}

