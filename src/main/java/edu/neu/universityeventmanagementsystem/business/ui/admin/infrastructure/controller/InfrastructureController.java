package edu.neu.universityeventmanagementsystem.business.ui.admin.infrastructure.controller;

import edu.neu.universityeventmanagementsystem.business.beans.CurrentUserBean;
import edu.neu.universityeventmanagementsystem.business.entity.*;
import edu.neu.universityeventmanagementsystem.business.service.AdminWingService;
import edu.neu.universityeventmanagementsystem.business.service.CollegesService;
import edu.neu.universityeventmanagementsystem.business.service.CouncilsService;
import edu.neu.universityeventmanagementsystem.business.service.ProgramsService;
import edu.neu.universityeventmanagementsystem.business.ui.admin.infrastructure.view.InfrastructureView;
import edu.neu.universityeventmanagementsystem.business.ui.shared.controller.FormController;
import edu.neu.universityeventmanagementsystem.business.ui.shared.controller.InnerViewController;
import edu.neu.universityeventmanagementsystem.business.util.ConstantValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * InfrastructureController class
 *
 * @author Raghavan Renganathan <renganathan.r@husky.neu.edu>
 * @version 1.0
 * @since 4/13/2018
 */
@Controller
@Lazy
public final class InfrastructureController extends FormController implements InnerViewController {

    private InfrastructureView infrastructureView;
    private CollegesService collegesService;
    private ProgramsService programsService;
    private AdminWingService adminWingService;
    private CouncilsService councilsService;
    private CollegesEntity selectedCollege;
    private CurrentUserBean currentUserBean;
    private int currentView;

    @Autowired
    public InfrastructureController(InfrastructureView infrastructureView, CollegesService collegesService,
                                    ProgramsService programsService, AdminWingService adminWingService,
                                    CouncilsService councilsService, CurrentUserBean currentUserBean) {
        this.infrastructureView = infrastructureView;
        this.collegesService = collegesService;
        this.programsService = programsService;
        this.adminWingService = adminWingService;
        this.councilsService = councilsService;
        this.currentUserBean = currentUserBean;
        currentView = 0;
        selectedCollege = null;
    }

    @Override
    public void prepareAndOpenForm() {
        registerEvents();
        populateMainList();
        restrictView();
    }

    private void restrictView() {
        RolesEntity role = currentUserBean.getCurrentUser().getRolesByIdRole();
        if (role.getPrivilegeLevel() < ConstantValues.RolePrivilegeLevel.SYSTEM_ADMIN)
            infrastructureView.hideAddDeleteCollegesControl();

        if (role.getPrivilegeLevel() < ConstantValues.RolePrivilegeLevel.NETWORK_ADMIN) {
            for (JButton jButton : infrastructureView.getPanelButtons()) {
                jButton.setVisible(false);
            }
            infrastructureView.setEnterpriseLabel(role.getHierarchyByIdHierarchy().getTableName());
            switch (role.getHierarchyByIdHierarchy().getTableName()) {
                case ConstantValues.Hierarchy.PROGRAM:
                    currentView = 0;
                    break;
                case ConstantValues.Hierarchy.ADMIN_WING:
                    currentView = 1;
                    break;
                case ConstantValues.Hierarchy.COUNCIL:
                    currentView = 2;
                    break;
            }
        }
    }

    private void populateMainList() {
        List<String> colleges = new ArrayList<>();
        RolesEntity role = currentUserBean.getCurrentUser().getRolesByIdRole();
        if (role.getPrivilegeLevel() == ConstantValues.RolePrivilegeLevel.SYSTEM_ADMIN) {
            collegesService.findAll().forEach(collegesEntity -> colleges.add(collegesEntity.getName()));
        } else {
            colleges.add(currentUserBean.getCurrentUser().getCollegeMembersByIdUser().getCollegesByIdCollege().getName());
        }

        infrastructureView.populateLists(colleges, InfrastructureView.MAIN_LIST_INDEX);
    }

    private void populateSubLists() {
        if (selectedCollege == null) return;

        List<String> sublistEntities = new ArrayList<>();

        switch (currentView) {
            case 0:
                selectedCollege.getProgramsByIdCollege().forEach(programsEntity -> sublistEntities.add(programsEntity.getName()));
                break;
            case 1:
                selectedCollege.getAdminWingsByIdCollege().forEach(adminWingEntity -> sublistEntities.add(adminWingEntity.getName()));
                break;
            case 2:
                selectedCollege.getCouncilsByIdCollege().forEach(councilsEntity -> sublistEntities.add(councilsEntity.getName()));
                break;
            default:
        }
        infrastructureView.populateLists(sublistEntities, InfrastructureView.SUB_LIST_INDEX);
    }

    @Override
    public Component getView() {
        prepareAndOpenForm();
        return infrastructureView;
    }

    private void registerEvents() {
        registerListSelection(infrastructureView.getListComponent(InfrastructureView.MAIN_LIST_INDEX),
                this::selectedCollegeChanged);
        infrastructureView.getPanelButtons().forEach(button -> {
            registerAction(button, this::changeView);
        });

        infrastructureView.getAddButtons().forEach(button -> {
            registerAction(button, this::addEntity);
        });

        infrastructureView.getRemoveButtons().forEach(button -> {
            registerAction(button, this::removeEntity);
        });
    }

    private void addEntity(java.awt.event.ActionEvent event) {
        JButton button = (JButton) event.getSource();
        String buttonIdentifier = button.getName();

        if (InfrastructureView.MAIN_LIST_IDENTIFIER.equals(buttonIdentifier)) {
            addCollege();
            populateMainList();
        } else {
            if (currentView == 0)
                addProgram();
            else if (currentView == 1)
                addAdminWing();
            else
                addCouncil();
            selectedCollege = collegesService.findOneByName(selectedCollege.getName());
            populateSubLists();
        }
    }

    private void addCollege() {
        String name = showInputBox("College");

        while (true) {
            if (name == null)
                return;
            if ("".equals(name)) {
                name = showInputBox("College");
            } else {
                break;
            }
        }

        CollegesEntity collegesEntity = collegesService.create();
        collegesEntity.setName(name);

        collegesService.save(collegesEntity);

        showMessageBox(String.format("Adding College(%s)", name), "success");
    }

    private void addAdminWing() {
        if (selectedCollege == null) {
            showMessageBox("Cannot add an admin wing", "Select a college first");
            return;
        }
        String name = showInputBox("Admin Wing");

        while (true) {
            if (name == null)
                return;
            if ("".equals(name)) {
                name = showInputBox("Admin Wing");
            } else {
                break;
            }
        }

        AdminWingEntity adminWingEntity = adminWingService.create();
        adminWingEntity.setName(name);
        adminWingEntity.setCollegesByIdCollege(selectedCollege);

        adminWingService.save(adminWingEntity);

        showMessageBox(String.format("Adding Admin Wing(%s)", name), "success");
    }

    private void addCouncil() {
        if (selectedCollege == null) {
            showMessageBox("Cannot add a council", "Select a college first");
            return;
        }
        String name = showInputBox("Council");

        while (true) {
            if (name == null)
                return;
            if ("".equals(name)) {
                name = showInputBox("Council");
            } else {
                break;
            }
        }

        CouncilsEntity councilsEntity = councilsService.create();
        councilsEntity.setName(name);
        councilsEntity.setCollegesByIdCollege(selectedCollege);

        councilsService.save(councilsEntity);

        showMessageBox(String.format("Adding Council(%s)", name), "success");
    }

    private void addProgram() {
        if (selectedCollege == null) {
            showMessageBox("Cannot add a program", "Select a college first");
            return;
        }
        String name = showInputBox("Program");

        while (true) {
            if (name == null)
                return;
            if ("".equals(name)) {
                name = showInputBox("Program");
            } else {
                break;
            }
        }

        ProgramsEntity programsEntity = programsService.create();
        programsEntity.setName(name);
        programsEntity.setCollegesByIdCollege(selectedCollege);

        programsService.save(programsEntity);

        showMessageBox(String.format("Adding Program(%s)", name), "success");
    }

    private void removeEntity(java.awt.event.ActionEvent event) {
        JButton button = (JButton) event.getSource();
        String buttonIdentifier = button.getName();

        if (InfrastructureView.MAIN_LIST_IDENTIFIER.equals(buttonIdentifier)) {
            removeCollege();
            populateMainList();
            infrastructureView.clearList(InfrastructureView.SUB_LIST_INDEX);
        } else {
            if (currentView == 0)
                removeProgram();
            else if (currentView == 1)
                removeAdminWing();
            else
                removeCouncil();
            populateSubLists();
        }
    }

    private void removeCollege() {
        if (showConfirmationBox(String.format("Do you want to delete the college \"%s\"", selectedCollege.getName()), "Confirm deletion")
                == JOptionPane.YES_OPTION) {
            collegesService.delete(selectedCollege);
            showMessageBox(String.format("College \"%s\" deletion", selectedCollege.getName()), "Success");
            selectedCollege = null;
        }
    }

    private void removeProgram() {
        JList list = infrastructureView.getListComponent(InfrastructureView.SUB_LIST_INDEX);

        if (list.isSelectionEmpty()) {
            showMessageBox("Cannot delete a program", "Select a program first");
            return;
        }

        String name = list.getSelectedValue().toString();
        if (showConfirmationBox(String.format("Do you want to delete the program \"%s\"", name), "Confirm deletion")
                == JOptionPane.YES_OPTION) {
            programsService.deleteByName(name);
            showMessageBox(String.format("Program \"%s\" deletion", name), "Success");
            selectedCollege = collegesService.findOneByName(selectedCollege.getName());
            populateSubLists();
        }
    }

    private void removeAdminWing() {
        JList list = infrastructureView.getListComponent(InfrastructureView.SUB_LIST_INDEX);

        if (list.isSelectionEmpty()) {
            showMessageBox("Cannot delete an admin wing", "Select an admin wing first");
            return;
        }

        String name = list.getSelectedValue().toString();
        if (showConfirmationBox(String.format("Do you want to delete the admin wing \"%s\"", name), "Confirm deletion")
                == JOptionPane.YES_OPTION) {
            adminWingService.deleteByName(name);
            showMessageBox(String.format("Admin Wing \"%s\" deletion", name), "Success");
            selectedCollege = collegesService.findOneByName(selectedCollege.getName());
            populateSubLists();
        }
    }

    private void removeCouncil() {
        JList list = infrastructureView.getListComponent(InfrastructureView.SUB_LIST_INDEX);

        if (list.isSelectionEmpty()) {
            showMessageBox("Cannot delete a council", "Select a council first");
            return;
        }

        String name = list.getSelectedValue().toString();
        if (showConfirmationBox(String.format("Do you want to delete the council \"%s\"", name), "Confirm deletion")
                == JOptionPane.YES_OPTION) {
            councilsService.deleteByName(name);
            showMessageBox(String.format("Council \"%s\" deletion", name), "Success");
            selectedCollege = collegesService.findOneByName(selectedCollege.getName());
            populateSubLists();
        }
    }

    private void selectedCollegeChanged(javax.swing.event.ListSelectionEvent event) {
        JList list = (JList) event.getSource();

        if (list.isSelectionEmpty()) return;

        String selected = list.getSelectedValue().toString();
        selectedCollege = collegesService.findOneByName(selected);

        populateSubLists();
    }


    private void changeView(java.awt.event.ActionEvent event) {
        String view = ((javax.swing.JButton) event.getSource()).getText();
        infrastructureView.setActiveButton((javax.swing.JButton) event.getSource());

        switch (view) {
            case "Programs":
                currentView = 0;
                populateSubLists();
                break;
            case "Administration":
                currentView = 1;
                populateSubLists();
                break;
            case "Councils":
                currentView = 2;
                populateSubLists();
                break;
            default:
        }
    }

    private String showInputBox(String name) {
        return JOptionPane.showInputDialog(null, String.format("Please enter a name for the new \"%s\"", name));
    }

    private void showMessageBox(String message, String status) {
        JOptionPane.showMessageDialog(null, String.format("%s: %s", message, status));
    }

    private int showConfirmationBox(String message, String title) {
        return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
    }
}
