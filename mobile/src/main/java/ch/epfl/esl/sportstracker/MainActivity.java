package ch.epfl.esl.sportstracker;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements MainFragment
        .OnFragmentInteractionListener, MyProfileFragment.OnFragmentInteractionListener,
        MyScheduleFragment.OnFragmentInteractionListener {

    private final String TAG = this.getClass().getSimpleName();

    private MainFragment mainFragment;
    private MyProfileFragment myProfileFragment;
    private MyScheduleFragment myHistoryFragment;
    private SectionsStatePagerAdapter mSectionStatePagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSectionStatePagerAdapter = new SectionsStatePagerAdapter(getSupportFragmentManager());

        // Do this in case of detaching of Fragments
        myProfileFragment = new MyProfileFragment();
        mainFragment      = new MainFragment();
        myHistoryFragment = new MyScheduleFragment();

        ViewPager mViewPager = findViewById(R.id.mainViewPager);
        setUpViewPager(mViewPager);

        // Set NewRecordingFragment as default tab once started the activity
        mViewPager.setCurrentItem(mSectionStatePagerAdapter.getPositionByTitle(getString(R.string
                .tab_title_main_fragment)));
    }

    private void setUpViewPager(ViewPager mViewPager) {
        mSectionStatePagerAdapter.addFragment(myProfileFragment, getString(R.string
                .tab_title_my_profile));
        mSectionStatePagerAdapter.addFragment(mainFragment, getString(R.string
                .tab_title_main_fragment));
        mSectionStatePagerAdapter.addFragment(myHistoryFragment, getString(R.string
                .tab_title_my_schedule));
        mViewPager.setAdapter(mSectionStatePagerAdapter);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
