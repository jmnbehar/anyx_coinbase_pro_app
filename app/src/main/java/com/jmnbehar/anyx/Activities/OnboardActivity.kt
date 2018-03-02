package com.jmnbehar.anyx.Activities

import android.animation.ArgbEvaluator
import android.graphics.Color
import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.anyx.R

import kotlinx.android.synthetic.main.activity_onboard.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.jmnbehar.anyx.Classes.Prefs
import kotlinx.android.synthetic.main.fragment_onboard.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.textColor


class OnboardActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager

    var nextBtn: ImageButton? = null
    var skipBtn: Button? = null
    var finishBtn:Button? = null

    var indicators: List<ImageView> = listOf()
    var lastLeftValue = 0

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

        var color1 = ResourcesCompat.getColor(resources, R.color.ltc_light, null)
        var color2 = ResourcesCompat.getColor(resources, R.color.btc_light, null)
        var color3 = ResourcesCompat.getColor(resources, R.color.gray_bg, null)
        var color4 = ResourcesCompat.getColor(resources, R.color.bch_light, null)
        var color5 = ResourcesCompat.getColor(resources, R.color.eth_light, null)

        var colorList = intArrayOf(color1, color2, color3, color4, color5)

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

        // Set up the ViewPager with the sections adapter.
        viewPager = home_view_pager
        viewPager.adapter = mSectionsPagerAdapter

        val evaluator = ArgbEvaluator()

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val colorPosition = if (position == (pageCount - 1)) {
                    position
                } else {
                    position + 1
                }

                val colorUpdate = evaluator.evaluate(positionOffset, colorList[position], colorList[colorPosition]) as Int
                viewPager.setBackgroundColor(colorUpdate)
            }

            override fun onPageSelected(position: Int) {
                currentPage = position

                when (position) {
                    0 -> viewPager.setBackgroundColor(color1)
                    1 -> viewPager.setBackgroundColor(color2)
                    2 -> viewPager.setBackgroundColor(color3)
                    3 -> viewPager.setBackgroundColor(color4)
                }

                for (i in 0..(indicators.count() - 1)) {
                    indicators[i].setImageResource(R.drawable.ic_launcher_background)
                }
                val imageIndicator = indicators[position]
                imageIndicator.setImageResource(R.drawable.white)

                nextBtn?.visibility = View.GONE // if (position == pageCount - 1) View.GONE else View.VISIBLE
                if (position == pageCount - 1) {
                    finishBtn?.text = "Continue"
                }
            }
        })
        nextBtn?.onClick {
            currentPage += 1
            viewPager.setCurrentItem(currentPage, true)
        }

        val prefs = Prefs(this)

        skipBtn?.onClick {
            finish()
            prefs.isFirstTime = false
        }

        finishBtn?.onClick {
            finish()
            prefs.isFirstTime = false
        }

    }



    override fun onBackPressed() {
        if (currentPage > 0) {
            currentPage -= 1
            viewPager.setCurrentItem(currentPage, true)
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

        val textColors: Array<Int> = arrayOf(Color.BLACK, Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE)
        val pageTitles: Array<String> = arrayOf(
                "Welcome to AnyX!",
                "Track Prices",
                "Your account",
                "Notifications",
                "The future")
        val pageStrings: Array<String> = arrayOf(
                "The best Android app for GDAX",
                "With this app you can keep up to date on Bitcoin, Ethereum, Litecoin, and Bitcoin Cash prices",
                "Create an API Key on the GDAX website to log in to this app. Once you're in, you'll be able to track your personal account.",
                "Set notifications to trigger if tokens reach specific price points, or if a rapid price change occurs.",
                "If you're still not impressed, stay tuned because full trading features will be added soon.",
                "Heads up: This app charges a 0.1% fee for taker orders. Maker orders remain free just as they are on the GDAX website.")
        val pageImages: Array<Int> = arrayOf(R.drawable.anyx_fg, R.drawable.coin_pile, R.drawable.chart, R.drawable.icon_alert_spaced, R.drawable.the_future)

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val position = arguments?.getInt(ARG_SECTION_NUMBER) ?: 0 - 1
            val rootView = inflater.inflate(R.layout.fragment_onboard, container, false)
//            textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
            rootView.section_title.text = pageTitles[position]
            rootView.section_label.text = pageStrings[position]
            rootView.section_title.textColor = textColors[position]
            rootView.section_label.textColor = textColors[position]
            rootView.image_view.setImageResource(pageImages[position])
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
