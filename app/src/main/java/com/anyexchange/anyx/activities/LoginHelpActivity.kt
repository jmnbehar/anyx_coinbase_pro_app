package com.anyexchange.anyx.activities

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
import com.anyexchange.anyx.R

import kotlinx.android.synthetic.main.activity_onboard.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.anyexchange.anyx.classes.Constants
import kotlinx.android.synthetic.main.fragment_onboard.view.*


class LoginHelpActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager

    var nextBtn: ImageButton? = null
    private var skipBtn: Button? = null
    var finishBtn:Button? = null

    var indicators: List<ImageView> = listOf()
    var isMobileHelpPage = true

    internal var currentPage = 0   //  to track page position
    val pageCount = 5

    private var sectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_login_help)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)


        indicators = listOf(intro_indicator_0, intro_indicator_1, intro_indicator_2, intro_indicator_3, intro_indicator_4, intro_indicator_5, intro_indicator_6, intro_indicator_7)

        for (i in (pageCount)..(indicators.count() - 1)) {
            indicators[i].visibility = View.GONE
        }

        indicators[0].setImageResource(R.drawable.white)
        nextBtn = intro_btn_next
        skipBtn = intro_btn_skip
        finishBtn = intro_btn_finish

        finishBtn?.visibility = View.VISIBLE
        finishBtn?.text = getString(R.string.login_help_skip_btn)

        isMobileHelpPage = intent.getBooleanExtra(Constants.isMobileLoginHelp, false)

        // Set up the ViewPager with the sections adapter.
        viewPager = home_view_pager
        viewPager.adapter = sectionsPagerAdapter

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
                    finishBtn?.text = getString(R.string.login_help_finish_btn)
                }
            }
        })
        nextBtn?.setOnClickListener {
            currentPage += 1
            viewPager.setCurrentItem(currentPage, true)
        }

        skipBtn?.setOnClickListener {
            finish()
        }

        finishBtn?.setOnClickListener {
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

    class PlaceholderFragment : Fragment() {
        private var mobileTitles = arrayOf<String>()
        private var mobileStrings = arrayOf<String>()
        private val mobileImages: Array<Int> = arrayOf(R.drawable.gdax_512, R.drawable.login_help_mobile_1,
                R.drawable.login_help_mobile_2, R.drawable.login_help_mobile_4, R.drawable.login_help_mobile_5)

        private var desktopTitles = arrayOf<String>()
        private var desktopStrings = arrayOf<String>()
        private val desktopImages: Array<Int> = arrayOf(R.drawable.gdax_512, R.drawable.login_help_desktop_1,
                R.drawable.login_help_desktop_2, R.drawable.login_help_desktop_3, R.drawable.login_help_desktop_4)

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            desktopTitles = resources.getStringArray(R.array.login_help_desktop_titles)
            desktopStrings = resources.getStringArray(R.array.login_help_desktop_text)

            mobileTitles = resources.getStringArray(R.array.login_help_mobile_titles)
            mobileStrings = resources.getStringArray(R.array.login_help_mobile_text)

            val position = (arguments?.getInt(ARG_SECTION_NUMBER) ?: 1) - 1
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
            private const val ARG_SECTION_NUMBER = "section_number"

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
