package org.usfirst.frc.team25.scouting.ui.dataentry;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;

import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import org.usfirst.frc.team25.scouting.R;
import org.usfirst.frc.team25.scouting.data.FileManager;
import org.usfirst.frc.team25.scouting.data.Settings;
import org.usfirst.frc.team25.scouting.data.models.PreMatch;
import org.usfirst.frc.team25.scouting.data.models.ScoutEntry;

import java.io.IOException;

public class PrematchFragment extends Fragment implements EntryFragment {

    private RadioButton[] startingPositionButtons;
    private Button continueButton;
    private MaterialEditText nameField, matchNumField, teamNumField;
    private MaterialBetterSpinner scoutPosSpinner;
    private ScoutEntry entry;


    public static PrematchFragment getInstance(ScoutEntry entry) {
        PrematchFragment prematchFragment = new PrematchFragment();
        prematchFragment.setEntry(entry);
        return prematchFragment;
    }

    @Override
    public ScoutEntry getEntry() {
        return entry;
    }

    public void setEntry(ScoutEntry entry) {
        this.entry = entry;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_prematch, container, false);

        startingPositionButtons = new RadioButton[3];
        continueButton = view.findViewById(R.id.prematch_continue);

        scoutPosSpinner = view.findViewById(R.id.scout_pos_spin);
        scoutPosSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.position_options)));
        scoutPosSpinner.setFloatingLabel(MaterialAutoCompleteTextView.FLOATING_LABEL_NORMAL);

        nameField = view.findViewById(R.id.scout_name_field);
        matchNumField = view.findViewById(R.id.match_num_field);
        teamNumField = view.findViewById(R.id.team_num_field);
        startingPositionButtons[0] = view.findViewById(R.id.leftStart);
        startingPositionButtons[1] = view.findViewById(R.id.centerStart);
        startingPositionButtons[2] = view.findViewById(R.id.rightStart);

        autoPopulate();

        // Nudges the user to fill in the next unfilled text field
        if (nameField.getText().toString().equals("")) {
            nameField.requestFocus();
        } else if (matchNumField.getText().toString().equals("")) {
            matchNumField.requestFocus();
        } else if (teamNumField.getText().toString().equals("")) {
            teamNumField.requestFocus();
        }


        continueButton.setOnClickListener(view1 -> {

            boolean proceed = true;
            Settings set = Settings.newInstance(getActivity());

            set.setMaxMatchNum(FileManager.getMaxMatchNum(getActivity()));

            // Sequentially verifies that user inputted a value
            if (nameField.getText().toString().equals("")) {
                nameField.setError("Scout nameField required");
                proceed = false;
            }

            if (scoutPosSpinner.getText().toString().equals("")) {
                scoutPosSpinner.setError("Scout position required");
                proceed = false;
            }

            if (matchNumField.getText().toString().equals("")) {
                matchNumField.setError("Match number required");
                proceed = false;

            } else if (Integer.parseInt(matchNumField.getText().toString()) < 1 ||
                    Integer.parseInt(matchNumField.getText().toString()) > Settings.newInstance(getActivity()).getMaxMatchNum()) {
                matchNumField.setError("Invalid match number value");
                proceed = false;
            }


            if (teamNumField.getText().toString().length() == 0 || Integer.parseInt(teamNumField.getText().toString()) < 1
                    || Integer.parseInt(teamNumField.getText().toString()) > 9999) {
                if (teamNumField.getText().length() == 0) {
                    teamNumField.setError("Team number required");
                } else {
                    teamNumField.setError("Invalid team number");
                }
                proceed = false;
            }

            boolean aButtonSelected = false;

            for (RadioButton button : startingPositionButtons) {
                if (button.isChecked())
                    aButtonSelected = true;
            }

            if (proceed && !aButtonSelected) {
                proceed = false;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Select robot starting position")
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, id) -> {
                            //do things
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }

            // If all normal checks pass, verify against team lists or match schedule
            if (Settings.newInstance(getActivity()).useTeamList() && proceed) {
                boolean checkTeamList = false;
                try {

                    // Non-quals matches don't get checked against match schedule
                    if (!Settings.newInstance(getActivity()).getMatchType().equals("Q")) {
                        checkTeamList = true;
                    } else if (!FileManager.getTeamPlaying(getActivity(),
                            scoutPosSpinner.getText().toString(),
                            Integer.parseInt(matchNumField.getText().toString())).equals(teamNumField.getText().toString())) {

                        proceed = false;
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Confirm team number")
                                .setMessage("Are you sure that team " + teamNumField.getText().toString() + " is " +
                                        scoutPosSpinner.getText().toString() + " in match " + matchNumField.getText().toString() + "?")
                                .setPositiveButton("Yes", (dialog, which) -> {

                                    saveState();
                                    Settings.newInstance(getActivity()).autoSetPreferences(entry.getPreMatch());

                                    hideKeyboard();
                                    getFragmentManager().beginTransaction()
                                            .replace(android.R.id.content,
                                                    AutoFragment.getInstance(entry), "AUTO")
                                            .commit();
                                })
                                .setNegativeButton("No", (dialog, which) -> {
                                    // do nothing
                                })
                                .create()
                                .show();

                    }

                } catch (IOException e) {
                    //Match list does not exist; looking for team list
                    checkTeamList = true;
                }
                if (checkTeamList && !FileManager.isOnTeamlist(teamNumField.getText().toString(),
                        getActivity())) {
                    proceed = false;
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Confirm team number")
                            .setMessage("Are you sure that team " + teamNumField.getText().toString() + " is playing at " + set.getCurrentEvent() + "?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                FileManager.addToTeamList(teamNumField.getText().toString(),
                                        getActivity());

                                continueButton.callOnClick();
                            })
                            .setNegativeButton("No", (dialog, which) -> {

                            })
                            .create()
                            .show();
                }
            }


            if (proceed) {
                saveState();

                set.autoSetPreferences(entry.getPreMatch());

                autoSetTheme();

                hideKeyboard();
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, AutoFragment.getInstance(entry), "AUTO")
                        .commit();

            }
        });

        return view;
    }

    private void autoSetTheme() {
        switch (entry.getPreMatch().getTeamNum()) {
            case 2590:
                getActivity().setTheme(R.style.AppTheme_NoLauncher_Red);
                break;
            case 225:
                getActivity().setTheme(R.style.AppTheme_NoLauncher_Red);
                break;
            case 303:
                getActivity().setTheme(R.style.AppTheme_NoLauncher_Green);
                break;
            case 25:
                getActivity().setTheme(R.style.AppTheme_NoLauncher_Raider);
                break;
            case 1923:
                getActivity().setTheme(R.style.AppTheme_NoLauncher_Black);
                break;
            default:
                getActivity().setTheme(R.style.AppTheme_NoLauncher_Blue);
                break;
        }
    }

    public void autoPopulate() {

        //Manually filled data overrides preferences
        if (entry.getPreMatch() != null) {
            PreMatch prevPreMatch = entry.getPreMatch();

            nameField.setText(prevPreMatch.getScoutName());
            scoutPosSpinner.setText(prevPreMatch.getScoutPos());
            matchNumField.setText(String.valueOf(prevPreMatch.getMatchNum()));
            teamNumField.setText(String.valueOf(prevPreMatch.getTeamNum()));
            for (RadioButton button : startingPositionButtons)
                if (button.getText().equals(prevPreMatch.getStartingPos()))
                    button.setChecked(true);
        }

        // Pulls data values from preferences to automatically fill fields
        else {
            Settings set = Settings.newInstance(getActivity());

            if (!set.getScoutPos().equals("DEFAULT")) {
                scoutPosSpinner.setText(set.getScoutPos());

                if (set.useTeamList() && set.getMatchType().equals("Q")) {
                    try {
                        teamNumField.setText(FileManager.getTeamPlaying(getActivity(),
                                set.getScoutPos(), set.getMatchNum()));
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }

            //Scout nameField is prompted for after a shift ends, but not during the first match
            if ((!set.getScoutName().equals("DEFAULT") && !((set.getMatchNum() - 1) % set.getShiftDur() == 0)) || set.getMatchNum() == 1) {
                nameField.setText(set.getScoutName());

            }

            matchNumField.setText(String.valueOf(set.getMatchNum()));
        }
    }

    @Override
    public void saveState() {
        String startPos = "";
        for (RadioButton button : startingPositionButtons) {
            if (button.isChecked()) {
                startPos = (String) button.getText();
            }
        }

        entry.setPreMatch(new PreMatch(nameField.getText().toString(),
                scoutPosSpinner.getText().toString(),
                Integer.parseInt(matchNumField.getText().toString()),
                Integer.parseInt(teamNumField.getText().toString()), startPos));
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Add Entry - Pre-Match");
    }

    /**
     * Hides the keyboard in the next fragment
     */
    private void hideKeyboard() {
        try {
            InputMethodManager inputManager =
                    (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken()
                    , InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

}