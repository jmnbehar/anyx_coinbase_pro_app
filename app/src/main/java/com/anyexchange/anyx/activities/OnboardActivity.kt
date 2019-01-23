package com.anyexchange.anyx.activities

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
import com.anyexchange.anyx.R

import kotlinx.android.synthetic.main.activity_onboard.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.anyexchange.anyx.classes.Prefs
import kotlinx.android.synthetic.main.fragment_onboard.view.*
import org.jetbrains.anko.textColor


class OnboardActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager

    var nextBtn: ImageButton? = null
    private var skipBtn: Button? = null
    var finishBtn:Button? = null

    var indicators: List<ImageView> = listOf()

    internal var currentPage = 0   //  to track page position
    val pageCount = 5

    private var sectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboard)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        val color1 = ResourcesCompat.getColor(resources, R.color.ltc_light, null)
        val color2 = ResourcesCompat.getColor(resources, R.color.btc_light, null)
        val color3 = ResourcesCompat.getColor(resources, R.color.gray_bg, null)
        val color4 = ResourcesCompat.getColor(resources, R.color.bch_light, null)
        val color5 = ResourcesCompat.getColor(resources, R.color.eth_light, null)

        val colorList = intArrayOf(color1, color2, color3, color4, color5)

        indicators = listOf(intro_indicator_0, intro_indicator_1, intro_indicator_2, intro_indicator_3, intro_indicator_4, intro_indicator_5, intro_indicator_6, intro_indicator_7)

        for (i in (pageCount)..(indicators.count() - 1)) {
            indicators[i].visibility = View.GONE
        }

        indicators[0].setImageResource(R.drawable.white)
        nextBtn = intro_btn_next
        skipBtn = intro_btn_skip
        finishBtn = intro_btn_finish

        finishBtn?.visibility = View.VISIBLE
        finishBtn?.text = resources.getString(R.string.onboard_skip)

        // Set up the ViewPager with the sections adapter.
        viewPager = home_view_pager
        viewPager.adapter = sectionsPagerAdapter

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
                    finishBtn?.text = resources.getString(R.string.onboard_continue)
                }
            }
        })
        nextBtn?.setOnClickListener {
            currentPage += 1
            viewPager.setCurrentItem(currentPage, true)
        }

        val prefs = Prefs(this)

        skipBtn?.setOnClickListener {
            finish()
            prefs.isFirstTime = false
        }

        finishBtn?.setOnClickListener {
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

        private val textColors: Array<Int> = arrayOf(Color.BLACK, Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE)
        private val pageTitles = resources.getStringArray(R.array.onboarding_titles)
        private val pageStrings: Array<String> = arrayOf(
                "The best Android app for Coinbase Pro",
                "With this app you can keep up to date on Bitcoin, Ethereum, Litecoin, and Bitcoin Cash prices",
                "Create an API Key on the Coinbase Pro website to log in to this app. Once you're in, you'll be able to track your personal account.",
                "Set notifications to trigger if tokens reach specific price points, or if a rapid price change occurs.",
                "Buy or sell any cryptocurrencies available on Coinbase Pro. You can make market, limit, or stop orders. Maker orders pay no fees!",
                "Heads up: This app charges a 0.1% fee for taker orders. Maker orders remain free just as they are on the Coinbase Pro website.")
        private val pageImages: Array<Int> = arrayOf(R.drawable.anyx_fg, R.drawable.coin_pile, R.drawable.chart, R.drawable.icon_alert_spaced, R.drawable.exchange)

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val position = (arguments?.getInt(ARG_SECTION_NUMBER) ?: 1) - 1
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
            private const val ARG_SECTION_NUMBER = "section_number"

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
