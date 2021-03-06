package edu.neu.universityeventmanagementsystem.business.ui.admin.landingpage.controller;

import edu.neu.universityeventmanagementsystem.business.beans.CurrentUserBean;
import edu.neu.universityeventmanagementsystem.business.entity.RolesEntity;
import edu.neu.universityeventmanagementsystem.business.entity.UsersEntity;
import edu.neu.universityeventmanagementsystem.business.ui.admin.dashboard.controller.AdminDashboardController;
import edu.neu.universityeventmanagementsystem.business.ui.admin.events.controller.EventsController;
import edu.neu.universityeventmanagementsystem.business.ui.admin.infrastructure.controller.InfrastructureController;
import edu.neu.universityeventmanagementsystem.business.ui.admin.landingpage.view.AdminLandingPanelView;
import edu.neu.universityeventmanagementsystem.business.ui.admin.users.controller.UsersController;
import edu.neu.universityeventmanagementsystem.business.ui.main.controller.MainFrameController;
import edu.neu.universityeventmanagementsystem.business.ui.shared.controller.FormController;
import edu.neu.universityeventmanagementsystem.business.util.ConstantMessages;
import edu.neu.universityeventmanagementsystem.business.util.ConstantValues;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

/**
 * AdminLandingPanelController class
 *
 * @author Raghavan Renganathan <renganathan.r@husky.neu.edu>
 * @version 1.0
 * @since 4/12/2018
 */
@Controller
@Lazy
public final class AdminLandingPanelController extends FormController {

    private final static Logger log = Logger.getLogger(AdminLandingPanelController.class);
    private MainFrameController mainFrameController;
    private AdminLandingPanelView landingPanelView;
    private CurrentUserBean currentUserBean;
    private ApplicationContext context;

    @Autowired
    public AdminLandingPanelController(MainFrameController mainFrameController,
                                       AdminLandingPanelView landingPanelView,
                                       CurrentUserBean currentUserBean,
                                       ApplicationContext context) {
        this.mainFrameController = mainFrameController;
        this.landingPanelView = landingPanelView;
        this.currentUserBean = currentUserBean;
        this.context = context;
    }

    @Override
    public void prepareAndOpenForm() {
        landingPanelView.reset();
        UsersEntity user = currentUserBean.getCurrentUser();

        if (user == null) {
            log.error("Current user is null");
            return;
        }
        registerAction(landingPanelView.getLogoutButton(), event -> doLogout());
        registerPanelEvents();
        landingPanelView.setUserText(user.getFirstName());
        restrictViewBasedOnPrivilege();
        loadDefaultView();
        viewPanel();
    }

    private void loadDefaultView() {
        landingPanelView.setTitle(ConstantMessages.Titles.ADMIN_DASHBOARD);
        landingPanelView.setContentPanel((context.getBean(AdminDashboardController.class)).getView());
    }

    private void restrictViewBasedOnPrivilege() {
        RolesEntity role = currentUserBean.getCurrentUser().getRolesByIdRole();

        if (role.getPrivilegeLevel() < ConstantValues.RolePrivilegeLevel.ENTERPRISE_ADMIN)
            landingPanelView.getPanelButtons().get(AdminLandingPanelView.INFRASTRUCTURE_BUTTON).setVisible(false);
    }

    private void registerPanelEvents() {
        landingPanelView.getPanelButtons().forEach(button -> {
            registerAction(button, this::changeView);
        });
    }

    private void changeView(java.awt.event.ActionEvent event) {
        String view = ((javax.swing.JButton) event.getSource()).getText();
        landingPanelView.setActiveButton((javax.swing.JButton) event.getSource());
        switch (view) {
            case "Dashboard":
                landingPanelView.setTitle(ConstantMessages.Titles.ADMIN_DASHBOARD);
                landingPanelView.setContentPanel((context.getBean(AdminDashboardController.class)).getView());
                break;
            case "Infrastructures":
                landingPanelView.setTitle(ConstantMessages.Titles.ADMIN_INFRASTRUCTURE);
                landingPanelView.setContentPanel((context.getBean(InfrastructureController.class)).getView());
                break;
            case "Users":
                landingPanelView.setTitle(ConstantMessages.Titles.ADMIN_USERS);
                landingPanelView.setContentPanel((context.getBean(UsersController.class)).getView());
                break;
            case "Events":
                landingPanelView.setTitle(ConstantMessages.Titles.ADMIN_EVENTS);
                landingPanelView.setContentPanel((context.getBean(EventsController.class)).getView());
                break;
            default:
        }
    }

    private void doLogout() {
        currentUserBean.setCurrentUser(null);

        mainFrameController.removeFromLayout(landingPanelView);
    }

    private void viewPanel() {
        mainFrameController.addToLayout(landingPanelView);
    }

}