package com.jmnbehar.anyx.Activities

import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.jmnbehar.anyx.R

import android.widget.Button
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.Fragments.Verify.VerifyEmailFragment
import com.jmnbehar.anyx.Fragments.Verify.VerifyIntroFragment
import com.jmnbehar.anyx.Fragments.Verify.VerifySendFragment
import kotlinx.android.synthetic.main.activity_verify.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast


class VerifyActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager

    var nextBtn:Button? = null

    var isMobileHelpPage = true

    internal var currentPage = 0   //  to track page position
    var pageCount = 1

    private var emailConfirmed = false

    var email = ""
    var amount = 0.0
    var currency: Currency? = null


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
        setContentView(R.layout.activity_verify)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        nextBtn = btn_verify_next

        nextBtn?.visibility = View.VISIBLE
        nextBtn?.text = "Next"

        isMobileHelpPage = intent.getBooleanExtra(Constants.isMobileLoginHelp, false)

        amount = intent.getDoubleExtra(Constants.verifyAmount, 0.0)

        // Set up the ViewPager with the sections adapter.
        viewPager = verify_view_pager
        viewPager.adapter = mSectionsPagerAdapter

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (position == 1 && positionOffset > 0 && !emailConfirmed) {
                    toast("Emails don't match")
                }
                // nextBtn?.text = "Done"
            }

            override fun onPageSelected(position: Int) {
                currentPage = position
                if (position == 2) {
                    nextBtn?.visibility = View.GONE
                    if (!emailConfirmed) {
                        toast("Emails don't match")
                    } else if (amount <= 0) {
                        toast("Server Error")
                    }
                    // nextBtn?.text = "Done"
                } else {
                    nextBtn?.visibility = View.VISIBLE
                }
            }
        })

        nextBtn?.onClick {
            if (currentPage == 1 && !emailConfirmed) {
                toast("Emails don't match")
            } else if (currentPage == 1 && amount <= 0) {
                toast("Server Error")
            } else {
                currentPage += 1
                viewPager.setCurrentItem(currentPage, true)
            }
        }

    }

    fun confirmEmail(email: String) {
        this.email = email
        emailConfirmed = true
        pageCount = 3
        viewPager.adapter?.notifyDataSetChanged()
    }

    fun blankEmail() {
        this.email = ""
        emailConfirmed = false
        pageCount = 2
        viewPager.adapter?.notifyDataSetChanged()
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
            return when (position) {
                0 -> VerifyIntroFragment.newInstance()
                1 -> VerifyEmailFragment.newInstance()
                2 -> VerifySendFragment.newInstance(email, amount, currency)
                else -> VerifyIntroFragment.newInstance()
            }
        }

        override fun getItemPosition(`object`: Any): Int {
            if (`object` is VerifySendFragment) {
                val verifySendFragment = `object` as VerifySendFragment
                verifySendFragment.updateViews()
            }
            return super.getItemPosition(`object`)
        }

        override fun getCount(): Int {
            // Show 4 total pages.
            return pageCount
        }
    }
}
