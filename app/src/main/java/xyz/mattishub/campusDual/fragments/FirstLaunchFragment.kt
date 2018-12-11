package xyz.mattishub.campusDual.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_first_launch.view.*
import xyz.mattishub.campusDual.R
import xyz.mattishub.campusDual.mainActivity

class FirstLaunchFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_first_launch, container, false)

        view.firstlaunch_confirm.setOnClickListener {
            findNavController().navigate(FirstLaunchFragmentDirections.actionFirstLaunchToSettings())
        }

        return view
    }

}
