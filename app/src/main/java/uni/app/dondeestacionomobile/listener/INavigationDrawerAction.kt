package uni.app.dondeestacionomobile.listener

interface INavigationDrawerAction {
    fun openDrawer()
    fun closeDrawer()
    fun lockDrawer()
    fun unlockDrawer()

    fun isEnabledEstacionamiento(): Boolean
    fun isEnabledZona(): Boolean
    fun isEnabledCorte(): Boolean
}
