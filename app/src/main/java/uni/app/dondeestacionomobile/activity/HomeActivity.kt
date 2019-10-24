package uni.app.dondeestacionomobile.activity

import android.os.Bundle
import android.view.MenuItem
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.navigation.NavigationView
import uni.app.dondeestacionomobile.R
import uni.app.dondeestacionomobile.databinding.ActivityHomeBinding
import uni.app.dondeestacionomobile.listener.INavigationDrawerAction


class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    INavigationDrawerAction {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var navigation: NavigationView
    private var mapSwitch: HashMap<Int, Switch> = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityHomeBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_home
        )
        setUi(binding)
    }

    private fun setUi(binding: ActivityHomeBinding) {
        drawerLayout = binding.drawerLayout
        navigation = binding.homeNavigation
        navController = findNavController(R.id.home_nav_fragment)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)

        navigation.setNavigationItemSelectedListener(this)
        navigation.menu.forEach { item ->
            run {
                if (item.hasSubMenu()) {
                    for (index in 0 until item.subMenu.size()) {
                        val actionView = item.subMenu[index].actionView
                        if (actionView is Switch) {
                            actionView.isChecked = true
                            mapSwitch[actionView.id] = actionView
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }

    override fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun lockDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun unlockDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    override fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun isEnabledEstacionamiento(): Boolean {
        return mapSwitch[R.id.option_estacionamiento]!!.isChecked
    }

    override fun isEnabledZona(): Boolean {
        return mapSwitch[R.id.option_zonas]!!.isChecked
    }

    override fun isEnabledCorte(): Boolean {
        return mapSwitch[R.id.option_cortes]!!.isChecked
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val actionView = when (item.itemId) {
            R.id.option_cortes -> item.actionView as Switch
            R.id.option_estacionamiento -> item.actionView as Switch
            R.id.option_zonas -> item.actionView as Switch
            else -> null
        }
        actionView?.isChecked = !actionView?.isChecked!!
        return true
    }
}
