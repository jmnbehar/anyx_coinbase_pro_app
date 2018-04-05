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
import com.jmnbehar.anyx.Fragments.Verify.VerifyCompleteFragment
import com.jmnbehar.anyx.Fragments.Verify.VerifySendFragment
import kotlinx.android.synthetic.main.activity_verify.*
import org.jetbrains.anko.sdk25.coroutines.onClick


class VerifyActivity : AppCompatActivity() {
    private lateinit var viewPager: LockableViewPager

    var nextBtn:Button? = null

    internal var currentPage = 0   //  to track page position
    var pageCount = 1

    var currency: Currency = Currency.BTC
    var verificationFundSource: VerificationFundSource? = null
    var verifyStatus: VerificationStatus? = null

    var blockBackButton = false


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

        nextBtn?.visibility = View.GONE

        val currencyStr = intent.getStringExtra(Constants.verifyCurrency) ?: "BTC"
        val fundSourceStr = intent.getStringExtra(Constants.verifyFundSource) ?: ""

        currency = Currency.forString(currencyStr) ?: Currency.BTC
        verificationFundSource = VerificationFundSource.fromString(fundSourceStr)

        // Set up the ViewPager with the sections adapter.
        viewPager = verify_view_pager
        viewPager.adapter = mSectionsPagerAdapter

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                currentPage = position
                when (position) {
                    0 -> nextBtn?.visibility = View.GONE
                    1 -> {
                        nextBtn?.visibility = View.VISIBLE
                        nextBtn?.text = "Done"
                        nextBtn?.onClick { finish() }    //TODO: maybe eventually go to sweep coinbase fragment
                    }
                }
            }
        })

        nextBtn?.onClick {
            currentPage += 1
            viewPager.setCurrentItem(currentPage, true)
        }

    }


    fun confirmEmail(email: String) {
        pageCount = 1
        viewPager.adapter?.notifyDataSetChanged()
    }

    fun blankEmail(failMessage: String) {
        pageCount = 1
        viewPager.adapter?.notifyDataSetChanged()
    }

    fun verificationComplete(verificationStatus: VerificationStatus) {
        verifyStatus = verificationStatus
        pageCount = 2
        currentPage = 1
        viewPager.adapter?.notifyDataSetChanged()
        viewPager.setCurrentItem(currentPage, true)
        viewPager.isLocked = true
        blockBackButton = true
    }

    override fun onBackPressed() {
        if (!blockBackButton) {
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
                0 -> VerifySendFragment.newInstance()
                1 -> VerifyCompleteFragment.newInstance()
                else -> VerifySendFragment.newInstance()
            }
        }

        override fun getItemPosition(`object`: Any): Int {
            if (`object` is VerifySendFragment) {
                `object`.updateViews()
            }
            if (`object` is VerifyCompleteFragment && verifyStatus != null) {
                `object`.updateText(verifyStatus!!)
            }
            return super.getItemPosition(`object`)
        }

        override fun getCount(): Int {
            // Show 4 total pages.
            return pageCount
        }
    }
}
