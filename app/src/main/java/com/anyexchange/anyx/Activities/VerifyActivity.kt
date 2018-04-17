package com.anyexchange.anyx.Activities

import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.anyexchange.anyx.R

import android.widget.Button
import com.anyexchange.anyx.Classes.*
import com.anyexchange.anyx.Fragments.Verify.VerifyCompleteFragment
import com.anyexchange.anyx.Fragments.Verify.VerifyIntroFragment
import com.anyexchange.anyx.Fragments.Verify.VerifySendFragment
import kotlinx.android.synthetic.main.activity_verify.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast


class VerifyActivity : AppCompatActivity() {
    private lateinit var viewPager: LockableViewPager

    var nextBtn:Button? = null

    internal var currentPage = 0   //  to track page position
    var pageCount = 2

    var currency: Currency = Currency.BTC
    var verificationFundSource: VerificationFundSource? = null  //not currently in use
    var verifyStatus: VerificationStatus? = null

    var blockBackButton = false

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        nextBtn = btn_verify_next

        nextBtn?.visibility = View.VISIBLE
        nextBtn?.text = "I Accept"
        nextBtn?.onClick {
            currentPage += 1
            viewPager.setCurrentItem(currentPage, true)
            nextBtn?.visibility = View.GONE
            viewPager.isLocked = false
        }


        val currencyStr = intent.getStringExtra(Constants.verifyCurrency) ?: ""
        val fundSourceStr = intent.getStringExtra(Constants.verifyFundSource) ?: ""

        currency = Currency.forString(currencyStr) ?: defaultVerificationCurrency
        verificationFundSource = VerificationFundSource.fromString(fundSourceStr)

        // Set up the ViewPager with the sections adapter.
        viewPager = verify_view_pager
        viewPager.adapter = mSectionsPagerAdapter

        viewPager.isLocked = true

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//                if (!isEulaAccepted && position == 1 && positionOffset > 0) {
//                    toast("Please accept agreement")
//                }
            }

            override fun onPageSelected(position: Int) {
                currentPage = position
                when (position) {
                    0 -> nextBtn?.visibility = View.VISIBLE
                    1 -> nextBtn?.visibility = View.GONE
                    2 -> {
                        nextBtn?.visibility = View.VISIBLE
                        nextBtn?.text = "Done"
                        nextBtn?.onClick { finish() }    //TODO: maybe eventually go to sweep coinbase fragment
                    }
                }
            }
        })
    }

    fun verificationComplete(verificationStatus: VerificationStatus) {
        verifyStatus = verificationStatus
        pageCount = 3
        currentPage = (pageCount - 1)
        viewPager.adapter?.notifyDataSetChanged()
        viewPager.setCurrentItem(currentPage, true)
        viewPager.isLocked = true
        blockBackButton = true
    }

//    fun acceptEula() {
//        viewPager.adapter?.notifyDataSetChanged()
//
//        nextBtn?.visibility = View.VISIBLE
//        nextBtn?.text = "I Accept"
//
//        nextBtn?.onClick {
//            pageCount = 2
//            currentPage += 1
//            viewPager.setCurrentItem(currentPage, true)
//            nextBtn?.visibility = View.GONE
//        }
//    }

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

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return when (position) {
                0 -> VerifyIntroFragment.newInstance()
                1 -> VerifySendFragment.newInstance()
                2 -> VerifyCompleteFragment.newInstance()
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
