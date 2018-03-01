package com.jmnbehar.anyx.Activities

import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.anyx.R

import kotlinx.android.synthetic.main.activity_onboard.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.jmnbehar.anyx.Classes.Constants
import com.jmnbehar.anyx.Classes.Prefs
import kotlinx.android.synthetic.main.fragment_onboard.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick


class LoginHelpActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager

    var nextBtn: ImageButton? = null
    var skipBtn: Button? = null
    var finishBtn:Button? = null

    var indicators: List<ImageView> = listOf()
    var isMobileHelpPage = true

    internal var currentPage = 0   //  to track page position
    val pageCount = 5

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboard)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)


        indicators = listOf(intro_indicator_0, intro_indicator_1, intro_indicator_2, intro_indicator_3, intro_indicator_4, intro_indicator_5, intro_indicator_6, intro_indicator_7)

        for (i in (pageCount)..(indicators.count() - 1)) {
            indicators[i].visibility = View.GONE
        }

        val imageIndicator = indicators[0]
        indicators[0].setImageResource(R.drawable.white)
        nextBtn = intro_btn_next
        skipBtn = intro_btn_skip
        finishBtn = intro_btn_finish

        finishBtn?.visibility = View.VISIBLE
        finishBtn?.text = "Skip"

        isMobileHelpPage = intent.getBooleanExtra(Constants.isMobileLoginHelp, false)

        // Set up the ViewPager with the sections adapter.
        viewPager = home_view_pager
        viewPager.adapter = mSectionsPagerAdapter

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                currentPage = position

                for (i in 0..(indicators.count() - 1)) {
                    indicators[i].setImageResource(R.drawable.ic_launcher_background)
                }

                val imageIndicator = indicators[position]
                imageIndicator.setImageResource(R.drawable.white)
//                imageIndicator.setColorFilter(Color.WHITE)
//                imageIndicator.setImageResource(R.drawable.anyx_logo)
//                intro_indicator_1.setColorFilter(Color.WHITE)

                nextBtn?.visibility = View.GONE // if (position == pageCount - 1) View.GONE else View.VISIBLE
                finishBtn?.visibility = View.VISIBLE
                if (position == pageCount - 1) {
                    finishBtn?.text = "Done"
                }
            }
        })
        nextBtn?.onClick {
            currentPage += 1
            viewPager.setCurrentItem(currentPage, true)
        }

        skipBtn?.onClick {
            finish()
        }

        finishBtn?.onClick {
            finish()
        }

    }



    override fun onBackPressed() {
        if (currentPage > 0) {
            currentPage -= 1
            viewPager.setCurrentItem(currentPage, true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1)
        }

        override fun getCount(): Int {
            // Show 4 total pages.
            return pageCount
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {
        val mobileTitles: Array<String> = arrayOf(
                "Create a new API Key on your phone",
                "Open Menu",
                "Open API Page",
                "Create API Key",
                "Enter Info To AnyX")
        val mobileStrings: Array<String> = arrayOf(
                " ",
                "On the GDAX mobile site, click the menu icon in the upper right hand corner.",
                "Select API from the menu.",
                "On the API Page, select View, Transfer, Bypass Two-Factor Auth, and Trade permissions. You may also set your own Passphrase." +
                        "\nAs long as you select View you will be able to track your account holdings in the app, but due to the way AnyX processes trades you need the other three permissions to buy or sell assets.",
                "Copy the API key and API secret into AnyX's login screen. Save these in the app so you don't have to go through this process again. \nYou're ready to go!")
        val mobileImages: Array<Int> = arrayOf(R.drawable.gdax_512, R.drawable.login_help_mobile_1, R.drawable.login_help_mobile_2, R.drawable.login_help_mobile_4, R.drawable.login_help_mobile_5)

        val desktopTitles: Array<String> = arrayOf(
                "Create a new API Key on your computer",
                " ",
                " ",
                " ",
                " ",
                " ")
        val desktopStrings: Array<String> = arrayOf(
                " ",
                "Log in to the GDAX website and click the menu icon in the upper right hand corner.",
                "Choose API from the right hand menu.",
                "On the API Page, select View, Transfer, Bypass Two-Factor Auth, and Trade permissions. You may also set your own Passphrase.\n" +
                        "As long as you select View you will be able to track your account holdings in the app, but due to the way AnyX processes trades you need the other three permissions to buy or sell assets.",
                "Copy down the info into the AnyX login screen. Make sure you do not send this info on any unencrypted channels, because ")
        val desktopImages: Array<Int> = arrayOf(R.drawable.gdax_512, R.drawable.login_help_desktop_1, R.drawable.login_help_desktop_2, R.drawable.login_help_desktop_3, R.drawable.login_help_desktop_4)

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val position = arguments.getInt(ARG_SECTION_NUMBER) - 1
            val rootView = inflater.inflate(R.layout.fragment_onboard, container, false)
//            textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
            if ((activity as LoginHelpActivity).isMobileHelpPage) {
                rootView.section_title.text = mobileTitles[position]
                rootView.section_label.text = mobileStrings[position]
                rootView.image_view.setImageResource(mobileImages[position])
            } else {
                rootView.section_title.text = desktopTitles[position]
                rootView.section_label.text = desktopStrings[position]
                rootView.image_view.setImageResource(desktopImages[position])
            }
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }
}
