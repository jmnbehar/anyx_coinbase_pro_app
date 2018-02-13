package com.jmnbehar.gdax.Activities

import android.animation.ArgbEvaluator
import android.graphics.Color
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
import com.jmnbehar.gdax.R

import kotlinx.android.synthetic.main.activity_onboard.*
import kotlinx.android.synthetic.main.fragment_onboard.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.jmnbehar.gdax.Classes.Prefs
import org.jetbrains.anko.sdk25.coroutines.onClick


class OnboardActivity : AppCompatActivity() {
    var color1 = Color.CYAN
    var color2 = Color.YELLOW
    var color3 = Color.MAGENTA

    var colorList = intArrayOf(color1, color2, color3)
    lateinit var viewPager: ViewPager

    var nextBtn: ImageButton? = null
    var skipBtn: Button? = null
    var finishBtn:Button? = null

    var zero: ImageView? = null
    var one:ImageView? = null
    var two:ImageView? = null
    var indicators: Array<ImageView>? = null
    var lastLeftValue = 0

    internal var page = 0   //  to track page position

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

        nextBtn = intro_btn_next
        skipBtn = intro_btn_skip
        finishBtn = intro_btn_finish

        // Set up the ViewPager with the sections adapter.
        viewPager = view_pager
        viewPager.adapter = mSectionsPagerAdapter

        val evaluator = ArgbEvaluator()

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val colorPosition = if (position == 2) {
                    position
                } else {
                    position + 1
                }

                val colorUpdate = evaluator.evaluate(positionOffset, colorList[position], colorList[colorPosition]) as Int
                viewPager.setBackgroundColor(colorUpdate)
            }

            override fun onPageSelected(position: Int) {
                page = position

              //  updateIndicators(page)

                when (position) {
                    0 -> viewPager.setBackgroundColor(color1)
                    1 -> viewPager.setBackgroundColor(color2)
                    2 -> viewPager.setBackgroundColor(color3)
                }


                nextBtn?.visibility = if (position == 2) View.GONE else View.VISIBLE
                finishBtn?.visibility = if (position == 2) View.VISIBLE else View.GONE
            }
        })
        nextBtn?.onClick {
            page += 1
            viewPager.setCurrentItem(page, true)
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
            // Show 3 total pages.
            return 3
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        val pageStrings: Array<String> = arrayOf("Hello and welcome to my Gdax App! Its in early alpha so I'm sure lots of stuff is broken still",
                "Theres nothing on this page go to the next one",
                "You need a Gdax API key to use this app. I'll add instructions later, but my advice is to log into gdax in a browser and make a new api key and copy the info over." +
                        "More updates to come...")
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_onboard, container, false)
//            textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
            rootView.section_label.text = pageStrings[arguments.getInt(ARG_SECTION_NUMBER)]
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
